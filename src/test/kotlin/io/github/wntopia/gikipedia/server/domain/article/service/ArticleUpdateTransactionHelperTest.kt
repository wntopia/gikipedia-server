package io.github.wntopia.gikipedia.server.domain.article.service

import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleJpaEntity
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.domain.history.service.ArticleHistoryRecorder
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.test.util.ReflectionTestUtils
import team.themoment.sdk.exception.ExpectedException
import java.time.Instant

/**
 * article 수정 세 곳(UpdateArticleServiceImpl 등)이 공유하는 "행 잠금 조회(404) → mutate → 리비전 기록 →
 * 캐시 반영" 시퀀스 자체를 검증. editor 지연 평가가 404 판정 이후로 미뤄지는지가 핵심이다.
 */
class ArticleUpdateTransactionHelperTest {
    private val articleRepository = mock<ArticleRepository>()
    private val articleHistoryRecorder = mock<ArticleHistoryRecorder>()
    private val articleCacheStore = mock<ArticleCacheStore>()

    private val helper = ArticleUpdateTransactionHelper(articleRepository, articleHistoryRecorder, articleCacheStore)

    private val article = ArticleJpaEntity(title = "제목", content = "이전 내용", imageUrl = "old.png")

    @BeforeEach
    fun setUp() {
        ReflectionTestUtils.setField(article, "id", 1L)
        ReflectionTestUtils.setField(article, "createdAt", Instant.now())
        ReflectionTestUtils.setField(article, "updatedAt", Instant.now())
        whenever(articleRepository.findByIdForUpdate(1L)).thenReturn(article)
    }

    @Test
    @DisplayName("존재하지 않는 article이면 editorProvider를 호출하기도 전에 404 예외를 던진다")
    fun notFoundThrowsBeforeResolvingEditor() {
        whenever(articleRepository.findByIdForUpdate(1L)).thenReturn(null)
        val editorProvider = mock<() -> String>()

        assertThatThrownBy {
            helper.update(1L, editorProvider) { it.content to it.content }
        }.isInstanceOf(ExpectedException::class.java)
        verifyNoInteractions(editorProvider)
    }

    @Test
    @DisplayName("mutate가 반환한 before/after로 리비전을 기록하고, 결과 리비전을 그대로 반환한다")
    fun recordsAndReturnsRevision() {
        whenever(articleHistoryRecorder.record(article, "이전 내용", "새 내용", "2412 홍길동")).thenReturn(5)

        val (response, revision) =
            helper.update(1L, { "2412 홍길동" }) {
                it.update("새 내용", it.imageUrl)
                "이전 내용" to "새 내용"
            }

        assertThat(revision).isEqualTo(5)
        assertThat(response.content).isEqualTo("새 내용")
    }

    @Test
    @DisplayName("응답을 커밋 이후 Redis 캐시에 채운다")
    fun cachesResponseAfterCommit() {
        val (response, _) = helper.update(1L, { "2412 홍길동" }) { it.content to it.content }

        verify(articleCacheStore).putAfterCommit(response)
    }

    @Test
    @DisplayName("행 잠금 조회(findByIdForUpdate)를 사용한다")
    fun usesLockedFetch() {
        helper.update(1L, { "2412 홍길동" }) { it.content to it.content }

        verify(articleRepository).findByIdForUpdate(1L)
        verify(articleRepository, org.mockito.kotlin.never()).findById(any())
    }
}
