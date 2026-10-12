package io.github.wntopia.gikipedia.server.domain.article.service.impl

import io.github.wntopia.gikipedia.server.domain.article.dto.request.CreateArticleReqDto
import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleJpaEntity
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.domain.article.service.ArticleCacheStore
import io.github.wntopia.gikipedia.server.domain.auth.dto.response.DataGsmUserInfoResDto
import io.github.wntopia.gikipedia.server.domain.history.event.ArticleUpdatedEvent
import io.github.wntopia.gikipedia.server.domain.user.service.ContributionRecorder
import io.github.wntopia.gikipedia.server.global.security.session.AuthenticationReader
import io.github.wntopia.gikipedia.server.global.storage.SeaweedUploader
import jakarta.servlet.http.HttpSession
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationEventPublisher
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.test.util.ReflectionTestUtils
import team.themoment.sdk.exception.ExpectedException
import java.time.Instant

/** 생성 시에도 Mongo 동기화 이벤트(baseline revision=1)를 발행하고 캐시를 채우는지 검증. */
class CreateArticleServiceImplTest {
    private val articleRepository = mock<ArticleRepository>()
    private val seaweedUploader = mock<SeaweedUploader>()
    private val eventPublisher = mock<ApplicationEventPublisher>()
    private val articleCacheStore = mock<ArticleCacheStore>()
    private val authenticationReader = mock<AuthenticationReader>()
    private val contributionRecorder = mock<ContributionRecorder>()
    private val session = mock<HttpSession>()

    private val service =
        CreateArticleServiceImpl(
            articleRepository,
            seaweedUploader,
            eventPublisher,
            articleCacheStore,
            authenticationReader,
            contributionRecorder,
        )

    @BeforeEach
    fun setUp() {
        whenever(articleRepository.saveAndFlush(any<ArticleJpaEntity>())).thenAnswer { invocation ->
            val entity = invocation.getArgument<ArticleJpaEntity>(0)
            ReflectionTestUtils.setField(entity, "id", 1L)
            // 이벤트 발행 시 createdAt/updatedAt을 requireNotNull로 확인하므로, 실제 저장 시
            // JPA 감사가 채우는 값을 mock 응답에서도 미리 채워둔다.
            ReflectionTestUtils.setField(entity, "createdAt", Instant.now())
            ReflectionTestUtils.setField(entity, "updatedAt", Instant.now())
            entity
        }
    }

    @Test
    @DisplayName("생성 시 revision=1 baseline 이벤트를 발행한다")
    fun publishesBaselineEvent() {
        service.execute(CreateArticleReqDto(title = "제목", content = "내용"), session)

        val captor = argumentCaptor<ArticleUpdatedEvent>()
        verify(eventPublisher).publishEvent(captor.capture())
        assertThat(captor.firstValue.articleId).isEqualTo(1L)
        assertThat(captor.firstValue.revision).isEqualTo(1)
    }

    @Test
    @DisplayName("생성 응답을 Redis 캐시에 채운다")
    fun cachesResponseAfterCommit() {
        val response = service.execute(CreateArticleReqDto(title = "제목", content = "내용"), session)

        verify(articleCacheStore).putAfterCommit(response)
    }

    @Test
    @DisplayName("제목은 정규화한 뒤 저장한다")
    fun savesNormalizedTitle() {
        service.execute(CreateArticleReqDto(title = "  기숙사   생활 ", content = "내용"), session)

        val captor = argumentCaptor<ArticleJpaEntity>()
        verify(articleRepository).saveAndFlush(captor.capture())
        assertThat(captor.firstValue.title).isEqualTo("기숙사 생활")
    }

    @Test
    @DisplayName("이미 있는 제목이면 이미지를 올리기 전에 409를 던진다")
    fun rejectsDuplicateTitleBeforeUpload() {
        whenever(articleRepository.existsByTitle("제목")).thenReturn(true)

        assertThatThrownBy { service.execute(CreateArticleReqDto(title = "제목", content = "내용"), session) }
            .isInstanceOfSatisfying(ExpectedException::class.java) {
                assertThat(it.statusCode).isEqualTo(HttpStatus.CONFLICT)
            }
        verify(seaweedUploader, never()).upload(any(), any())
        verify(articleRepository, never()).saveAndFlush(any<ArticleJpaEntity>())
    }

    @Test
    @DisplayName("동시 생성으로 유니크 제약에 걸려도 409로 변환한다")
    fun convertsUniqueViolationToConflict() {
        whenever(
            articleRepository.saveAndFlush(any<ArticleJpaEntity>()),
        ).thenThrow(DataIntegrityViolationException("dup"))

        assertThatThrownBy { service.execute(CreateArticleReqDto(title = "제목", content = "내용"), session) }
            .isInstanceOfSatisfying(ExpectedException::class.java) {
                assertThat(it.statusCode).isEqualTo(HttpStatus.CONFLICT)
            }
    }

    @Test
    @DisplayName("로그인한 사용자가 만들면 생성자를 기록하고 생성 기여를 남긴다")
    fun recordsCreatorWhenLoggedIn() {
        whenever(
            authenticationReader.getCurrentUser(session),
        ).thenReturn(DataGsmUserInfoResDto(10L, null, "USER", true, null))

        service.execute(CreateArticleReqDto(title = "제목", content = "내용"), session)

        val captor = argumentCaptor<ArticleJpaEntity>()
        verify(articleRepository).saveAndFlush(captor.capture())
        assertThat(captor.firstValue.createdByUserId).isEqualTo(10L)
        verify(contributionRecorder).recordCreate(10L, 1L)
    }

    @Test
    @DisplayName("비로그인 생성은 생성자 없이 허용한다")
    fun allowsAnonymousCreate() {
        whenever(authenticationReader.getCurrentUser(session)).thenReturn(null)

        service.execute(CreateArticleReqDto(title = "제목", content = "내용"), session)

        val captor = argumentCaptor<ArticleJpaEntity>()
        verify(articleRepository).saveAndFlush(captor.capture())
        assertThat(captor.firstValue.createdByUserId).isNull()
        verify(contributionRecorder).recordCreate(null, 1L)
    }
}
