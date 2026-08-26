package io.github.wntopia.gikipedia.server.domain.article.service.impl

import io.github.wntopia.gikipedia.server.domain.article.dto.request.UpdateArticleReqDto
import io.github.wntopia.gikipedia.server.domain.article.dto.response.ArticleResDto
import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleJpaEntity
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.domain.article.service.ArticleCacheStore
import io.github.wntopia.gikipedia.server.domain.article.service.ArticleUpdateTransactionHelper
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
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionTemplate
import team.themoment.sdk.exception.ExpectedException
import java.time.Instant

/**
 * 수정 시 행 잠금 조회(findByIdForUpdate)를 쓰는지, R2 업로드가 잠금 전에 끝나는지 검증.
 * 행 잠금+기록+캐시 시퀀스 자체는 [ArticleUpdateTransactionHelper]의 실제 인스턴스를 그대로 통해서
 * 검증한다(mock으로 대체하지 않음) — 그래야 기존 검증(findByIdForUpdate 호출, 캐시 반영 등)이 그대로 유효하다.
 * 실제 잠금 동작(동시 트랜잭션 직렬화) 자체는 단위 테스트로 검증할 수 없어 통합 테스트 영역이다.
 */
class UpdateArticleServiceImplTest {
    private val articleRepository = mock<ArticleRepository>()
    private val r2Uploader = mock<R2Uploader>()
    private val articleHistoryRecorder = mock<ArticleHistoryRecorder>()
    private val authenticationReader = mock<AuthenticationReader>()
    private val articleCacheStore = mock<ArticleCacheStore>()
    private val transactionTemplate = mock<TransactionTemplate>()
    private val session = mock<HttpSession>()

    private val articleUpdateTransactionHelper =
        ArticleUpdateTransactionHelper(articleRepository, articleHistoryRecorder, articleCacheStore)

    private val service =
        UpdateArticleServiceImpl(
            r2Uploader,
            authenticationReader,
            transactionTemplate,
            articleUpdateTransactionHelper,
        )

    private val article = ArticleJpaEntity(title = "제목", content = "이전 내용", imageUrl = "old.png")

    @BeforeEach
    fun setUp() {
        ReflectionTestUtils.setField(article, "id", 1L)
        ReflectionTestUtils.setField(article, "createdAt", Instant.now())
        ReflectionTestUtils.setField(article, "updatedAt", Instant.now())
        whenever(articleRepository.findByIdForUpdate(1L)).thenReturn(article)
        whenever(authenticationReader.getEditorLabel(session)).thenReturn("2412 홍길동")
        // TransactionTemplate.execute는 실제 트랜잭션 매니저 없이, 콜백을 그 자리에서 바로 실행하는 것으로 대체한다.
        whenever(transactionTemplate.execute<Pair<ArticleResDto, Int?>>(any())).thenAnswer { invocation ->
            invocation
                .getArgument<TransactionCallback<Pair<ArticleResDto, Int?>>>(
                    0,
                ).doInTransaction(mock<TransactionStatus>())
        }
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
    @DisplayName("존재하지 않는 article이면 인증 정보를 조회하기도 전에 404로 실패한다(401보다 404가 우선)")
    fun notFoundTakesPrecedenceOverAuth() {
        whenever(articleRepository.findByIdForUpdate(1L)).thenReturn(null)

        assertThatThrownBy { service.execute(1L, UpdateArticleReqDto(content = "새 내용"), session) }
            .isInstanceOf(ExpectedException::class.java)
        verifyNoInteractions(authenticationReader)
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
