package io.github.wntopia.gikipedia.server.domain.collaboration.service

import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleJpaEntity
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.domain.article.service.ArticleCacheStore
import io.github.wntopia.gikipedia.server.domain.article.service.ArticleUpdateTransactionHelper
import io.github.wntopia.gikipedia.server.domain.history.service.ArticleHistoryRecorder
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.test.util.ReflectionTestUtils
import team.themoment.sdk.exception.ExpectedException
import java.time.Instant

/**
 * `save`/안전망 저장이 모두 재사용하는 서비스 검증: 행 잠금 조회, 404, imageUrl 유지, 리비전 반환,
 * 커밋 후 캐시 반영(UpdateArticleServiceImplTest와 같은 패턴).
 */
class SaveCollaborativeRevisionServiceTest {
    private val articleRepository = mock<ArticleRepository>()
    private val articleHistoryRecorder = mock<ArticleHistoryRecorder>()
    private val articleCacheStore = mock<ArticleCacheStore>()

    private val articleUpdateTransactionHelper =
        ArticleUpdateTransactionHelper(articleRepository, articleHistoryRecorder, articleCacheStore)
    private val service = SaveCollaborativeRevisionService(articleUpdateTransactionHelper)

    private val article = ArticleJpaEntity(title = "제목", content = "이전 내용", imageUrl = "old.png")

    @BeforeEach
    fun setUp() {
        ReflectionTestUtils.setField(article, "id", 1L)
        ReflectionTestUtils.setField(article, "createdAt", Instant.now())
        ReflectionTestUtils.setField(article, "updatedAt", Instant.now())
        whenever(articleRepository.findByIdForUpdate(1L)).thenReturn(article)
    }

    @Test
    @DisplayName("행 잠금 조회(findByIdForUpdate)로 article을 가져온다")
    fun usesLockedFetch() {
        service.save(1L, "새 내용", "2412 홍길동")

        verify(articleRepository).findByIdForUpdate(1L)
    }

    @Test
    @DisplayName("존재하지 않는 article이면 404 예외를 던진다")
    fun notFoundThrows() {
        whenever(articleRepository.findByIdForUpdate(1L)).thenReturn(null)

        assertThatThrownBy { service.save(1L, "새 내용", "2412 홍길동") }
            .isInstanceOf(ExpectedException::class.java)
    }

    @Test
    @DisplayName("기존 imageUrl은 그대로 유지한 채 content만 갱신한다")
    fun keepsImageUrl() {
        service.save(1L, "새 내용", "2412 홍길동")

        assertThat(article.imageUrl).isEqualTo("old.png")
        assertThat(article.content).isEqualTo("새 내용")
    }

    @Test
    @DisplayName("리비전 기록 결과를 그대로 반환한다")
    fun returnsRecordedRevision() {
        whenever(articleHistoryRecorder.record(article, "이전 내용", "새 내용", "2412 홍길동")).thenReturn(7)

        val revision = service.save(1L, "새 내용", "2412 홍길동")

        assertThat(revision).isEqualTo(7)
    }

    @Test
    @DisplayName("수정 응답을 커밋 이후 Redis 캐시에 채운다")
    fun cachesResponseAfterCommit() {
        service.save(1L, "새 내용", "2412 홍길동")

        verify(articleCacheStore).putAfterCommit(any())
    }
}
