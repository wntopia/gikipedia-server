package io.github.wntopia.gikipedia.server.domain.article.service.impl

import io.github.wntopia.gikipedia.server.domain.article.dto.request.UpdateArticleImageReqDto
import io.github.wntopia.gikipedia.server.domain.article.dto.response.ArticleResDto
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.multipart.MultipartFile
import team.themoment.sdk.exception.ExpectedException
import java.time.Instant

/** 이미지 전용 수정: content는 건드리지 않고 imageUrl만 바뀌며, 리비전은 새로 만들지 않는지 검증. */
class UpdateArticleImageServiceImplTest {
    private val articleRepository = mock<ArticleRepository>()
    private val r2Uploader = mock<R2Uploader>()
    private val articleHistoryRecorder = mock<ArticleHistoryRecorder>()
    private val authenticationReader = mock<AuthenticationReader>()
    private val articleCacheStore = mock<ArticleCacheStore>()
    private val transactionTemplate = mock<TransactionTemplate>()
    private val session = mock<HttpSession>()
    private val image = mock<MultipartFile>()

    private val service =
        UpdateArticleImageServiceImpl(
            articleRepository,
            r2Uploader,
            articleHistoryRecorder,
            authenticationReader,
            articleCacheStore,
            transactionTemplate,
        )

    private val article = ArticleJpaEntity(title = "제목", content = "본문", imageUrl = "old.png")

    @BeforeEach
    fun setUp() {
        ReflectionTestUtils.setField(article, "id", 1L)
        ReflectionTestUtils.setField(article, "createdAt", Instant.now())
        ReflectionTestUtils.setField(article, "updatedAt", Instant.now())
        whenever(articleRepository.findByIdForUpdate(1L)).thenReturn(article)
        whenever(authenticationReader.getEditorLabel(session)).thenReturn("2412 홍길동")
        whenever(r2Uploader.upload(image, "articles")).thenReturn("new.png")
        // TransactionTemplate.execute는 실제 트랜잭션 매니저 없이, 콜백을 그 자리에서 바로 실행하는 것으로 대체한다.
        whenever(transactionTemplate.execute<ArticleResDto>(any())).thenAnswer { invocation ->
            invocation.getArgument<TransactionCallback<ArticleResDto>>(0).doInTransaction(mock<TransactionStatus>())
        }
    }

    @Test
    @DisplayName("행 잠금 조회로 article을 가져오고 content는 그대로 유지한 채 imageUrl만 바꾼다")
    fun updatesImageUrlOnly() {
        val response = service.execute(1L, UpdateArticleImageReqDto(image), session)

        verify(articleRepository).findByIdForUpdate(1L)
        assertThat(response.content).isEqualTo("본문")
        assertThat(response.imageUrl).isEqualTo("new.png")
    }

    @Test
    @DisplayName("존재하지 않는 article이면 404 예외를 던진다")
    fun notFoundThrows() {
        whenever(articleRepository.findByIdForUpdate(1L)).thenReturn(null)

        assertThatThrownBy { service.execute(1L, UpdateArticleImageReqDto(image), session) }
            .isInstanceOf(ExpectedException::class.java)
    }

    @Test
    @DisplayName("content 변화가 없으므로 리비전을 새로 만들지 않고 이벤트 재발행용으로만 record를 호출한다")
    fun recordsWithoutContentChangeForEventReplay() {
        service.execute(1L, UpdateArticleImageReqDto(image), session)

        verify(articleHistoryRecorder).record(article, "본문", "본문", "2412 홍길동")
    }

    @Test
    @DisplayName("수정 응답을 커밋 이후 Redis 캐시에 채운다")
    fun cachesResponseAfterCommit() {
        val response = service.execute(1L, UpdateArticleImageReqDto(image), session)

        verify(articleCacheStore).putAfterCommit(response)
    }
}
