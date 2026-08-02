package io.github.wntopia.gikipedia.server.domain.article.service.impl

import io.github.wntopia.gikipedia.server.domain.article.dto.request.UpdateArticleReqDto
import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleJpaEntity
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.domain.article.service.ArticleCacheStore
import io.github.wntopia.gikipedia.server.domain.history.service.ArticleHistoryRecorder
import io.github.wntopia.gikipedia.server.global.security.session.AuthenticationReader
import io.github.wntopia.gikipedia.server.global.storage.R2Uploader
import jakarta.servlet.http.HttpSession
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.test.util.ReflectionTestUtils
import team.themoment.sdk.exception.ExpectedException
import java.time.Instant

/**
 * 수정 시 행 잠금 조회(findByIdForUpdate)를 쓰는지, R2 업로드가 잠금 전에 끝나는지 검증.
 * 실제 잠금 동작(동시 트랜잭션 직렬화) 자체는 단위 테스트로 검증할 수 없어 통합 테스트 영역이다.
 */
class UpdateArticleServiceImplTest {
    private val articleRepository = mock<ArticleRepository>()
    private val r2Uploader = mock<R2Uploader>()
    private val articleHistoryRecorder = mock<ArticleHistoryRecorder>()
    private val authenticationReader = mock<AuthenticationReader>()
    private val articleCacheStore = mock<ArticleCacheStore>()
    private val session = mock<HttpSession>()

    private val service =
        UpdateArticleServiceImpl(
            articleRepository,
            r2Uploader,
            articleHistoryRecorder,
            authenticationReader,
            articleCacheStore,
        )

    private val article = ArticleJpaEntity(title = "제목", content = "이전 내용", imageUrl = "old.png")

    @BeforeEach
    fun setUp() {
        ReflectionTestUtils.setField(article, "id", 1L)
        ReflectionTestUtils.setField(article, "createdAt", Instant.now())
        ReflectionTestUtils.setField(article, "updatedAt", Instant.now())
        whenever(articleRepository.findByIdForUpdate(1L)).thenReturn(article)
        whenever(authenticationReader.getEditorLabel(session)).thenReturn("2412 홍길동")
    }

    @Test
    @DisplayName("행 잠금 조회(findByIdForUpdate)로 article을 가져오고, 잠금 없는 findById는 쓰지 않는다")
    fun usesLockedFetch() {
        service.execute(1L, UpdateArticleReqDto(content = "새 내용"), session)

        verify(articleRepository).findByIdForUpdate(1L)
        verify(articleRepository, never()).findById(any())
    }

    @Test
    @DisplayName("존재하지 않는 article이면 404 예외를 던진다")
    fun notFoundThrows() {
        whenever(articleRepository.findByIdForUpdate(1L)).thenReturn(null)

        assertThatThrownBy { service.execute(1L, UpdateArticleReqDto(content = "새 내용"), session) }
            .isInstanceOf(ExpectedException::class.java)
    }

    @Test
    @DisplayName("이미지가 없으면 기존 imageUrl을 유지한다")
    fun keepsExistingImageUrlWhenNoNewImage() {
        val response = service.execute(1L, UpdateArticleReqDto(content = "새 내용"), session)

        assertThat(response.imageUrl).isEqualTo("old.png")
        verify(r2Uploader, never()).upload(any(), any())
    }

    @Test
    @DisplayName("수정 응답을 커밋 이후 Redis 캐시에 채운다")
    fun cachesResponseAfterCommit() {
        val response = service.execute(1L, UpdateArticleReqDto(content = "새 내용"), session)

        verify(articleCacheStore).putAfterCommit(response)
    }
}
