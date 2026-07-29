package io.github.wntopia.gikipedia.server.domain.article.service

import io.github.wntopia.gikipedia.server.domain.article.dto.response.ArticleResDto
import io.github.wntopia.gikipedia.server.global.config.ArticleCacheEnvironment
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Duration
import java.time.Instant

/** putAfterCommit이 트랜잭션 유무에 따라 SET 시점을 올바르게 미루는지 검증. */
class ArticleCacheServiceTest {
    private val redisTemplate = mock<RedisTemplate<String, ArticleResDto>>()
    private val valueOps = mock<ValueOperations<String, ArticleResDto>>()
    private val service = ArticleCacheService(redisTemplate, ArticleCacheEnvironment(ttlMinutes = 60))

    private val article = ArticleResDto(1L, "제목", "내용", null, Instant.now(), Instant.now())

    init {
        whenever(redisTemplate.opsForValue()).thenReturn(valueOps)
    }

    @AfterEach
    fun cleanup() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization()
        }
    }

    @Test
    fun `트랜잭션 밖이면 즉시 SET한다`() {
        service.putAfterCommit(article)

        verify(valueOps).set(any<String>(), eq(article), any<Duration>())
    }

    @Test
    fun `트랜잭션 안이면 커밋 전까지 SET을 미루고, 커밋되면 그제서야 SET한다`() {
        TransactionSynchronizationManager.initSynchronization()

        service.putAfterCommit(article)
        verify(valueOps, never()).set(any<String>(), any<ArticleResDto>(), any<Duration>())

        TransactionSynchronizationManager.getSynchronizations().forEach { it.afterCommit() }
        verify(valueOps).set(any<String>(), eq(article), any<Duration>())
    }
}
