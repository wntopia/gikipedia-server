package io.github.wntopia.gikipedia.server.domain.article.service.impl

import io.github.wntopia.gikipedia.server.domain.article.dto.request.CreateArticleReqDto
import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleJpaEntity
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.domain.article.service.ArticleCacheService
import io.github.wntopia.gikipedia.server.domain.history.event.ArticleUpdatedEvent
import io.github.wntopia.gikipedia.server.global.storage.R2Uploader
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationEventPublisher
import org.springframework.test.util.ReflectionTestUtils

/** 생성 시에도 Mongo 동기화 이벤트(baseline revision=1)를 발행하고 캐시를 채우는지 검증. */
class CreateArticleServiceImplTest {
    private val articleRepository = mock<ArticleRepository>()
    private val r2Uploader = mock<R2Uploader>()
    private val eventPublisher = mock<ApplicationEventPublisher>()
    private val articleCacheService = mock<ArticleCacheService>()

    private val service = CreateArticleServiceImpl(articleRepository, r2Uploader, eventPublisher, articleCacheService)

    @BeforeEach
    fun setUp() {
        whenever(articleRepository.save(any<ArticleJpaEntity>())).thenAnswer { invocation ->
            val entity = invocation.getArgument<ArticleJpaEntity>(0)
            ReflectionTestUtils.setField(entity, "id", 1L)
            entity
        }
    }

    @Test
    @DisplayName("생성 시 revision=1 baseline 이벤트를 발행한다")
    fun publishesBaselineEvent() {
        service.execute(CreateArticleReqDto(title = "제목", content = "내용"))

        val captor = argumentCaptor<ArticleUpdatedEvent>()
        verify(eventPublisher).publishEvent(captor.capture())
        assertThat(captor.firstValue.articleId).isEqualTo(1L)
        assertThat(captor.firstValue.revision).isEqualTo(1)
    }

    @Test
    @DisplayName("생성 응답을 Redis 캐시에 채운다")
    fun cachesResponseAfterCommit() {
        val response = service.execute(CreateArticleReqDto(title = "제목", content = "내용"))

        verify(articleCacheService).putAfterCommit(response)
    }
}
