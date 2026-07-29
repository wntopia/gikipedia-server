package io.github.wntopia.gikipedia.server.domain.article.service.impl

import io.github.wntopia.gikipedia.server.domain.article.dto.response.ArticleResDto
import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleJpaEntity
import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleMongoEntity
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleMongoRepository
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.domain.article.service.ArticleCacheService
import io.github.wntopia.gikipedia.server.global.config.ArticleReadEnvironment
import io.github.wntopia.gikipedia.server.global.config.ArticleReadSource
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.test.util.ReflectionTestUtils
import team.themoment.sdk.exception.ExpectedException
import java.time.Instant
import java.util.Optional

/** 읽기 경로(feature flag, Redis→Mongo, MySQL 폴백 없음) 검증. */
class QueryArticleServiceImplTest {
    private val articleRepository = mock<ArticleRepository>()
    private val articleMongoRepository = mock<ArticleMongoRepository>()
    private val articleCacheService = mock<ArticleCacheService>()

    private val article = ArticleJpaEntity(title = "제목", content = "내용")

    @BeforeEach
    fun setUp() {
        ReflectionTestUtils.setField(article, "id", 1L)
    }

    private fun service(source: ArticleReadSource) =
        QueryArticleServiceImpl(
            articleRepository,
            articleMongoRepository,
            articleCacheService,
            ArticleReadEnvironment(source),
        )

    @Test
    @DisplayName("MYSQL 모드면 캐시/Mongo를 건드리지 않고 MySQL만 조회한다")
    fun mysqlModeQueriesMysqlOnly() {
        whenever(articleRepository.findById(1L)).thenReturn(Optional.of(article))

        val result = service(ArticleReadSource.MYSQL).execute(1L)

        assertThat(result.id).isEqualTo(1L)
        verify(articleCacheService, never()).get(org.mockito.kotlin.any())
        verify(articleMongoRepository, never()).findByDocumentId(org.mockito.kotlin.any())
    }

    @Test
    @DisplayName("REDIS_MONGO 모드에서 캐시 히트면 Mongo를 조회하지 않는다")
    fun cacheHitSkipsMongo() {
        val cached = ArticleResDto(1L, "제목", "내용", null, Instant.now(), Instant.now())
        whenever(articleCacheService.get(1L)).thenReturn(cached)

        val result = service(ArticleReadSource.REDIS_MONGO).execute(1L)

        assertThat(result).isEqualTo(cached)
        verify(articleMongoRepository, never()).findByDocumentId(org.mockito.kotlin.any())
    }

    @Test
    @DisplayName("캐시 미스면 Mongo를 조회하고, 결과를 캐시에 backfill한다")
    fun cacheMissFallsBackToMongoAndBackfills() {
        whenever(articleCacheService.get(1L)).thenReturn(null)
        val mongoEntity =
            ArticleMongoEntity(
                documentId = 1L,
                title = "제목",
                content = "내용",
                imageUrl = null,
                articleCreatedAt = Instant.now(),
                articleUpdatedAt = Instant.now(),
                revision = 2,
            )
        whenever(articleMongoRepository.findByDocumentId(1L)).thenReturn(mongoEntity)

        val result = service(ArticleReadSource.REDIS_MONGO).execute(1L)

        assertThat(result.id).isEqualTo(1L)
        verify(articleCacheService).put(result)
    }

    @Test
    @DisplayName("캐시와 Mongo 모두 미스면 MySQL로 폴백하지 않고 그대로 404를 던진다")
    fun bothMissThrowsNotFoundWithoutMysqlFallback() {
        whenever(articleCacheService.get(1L)).thenReturn(null)
        whenever(articleMongoRepository.findByDocumentId(1L)).thenReturn(null)

        assertThatThrownBy { service(ArticleReadSource.REDIS_MONGO).execute(1L) }
            .isInstanceOf(ExpectedException::class.java)
        verify(articleRepository, never()).findById(org.mockito.kotlin.any())
    }
}
