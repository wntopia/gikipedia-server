package io.github.wntopia.gikipedia.server.domain.article.service

import io.github.wntopia.gikipedia.server.domain.article.dto.response.ArticleResDto
import io.github.wntopia.gikipedia.server.global.config.ArticleCacheEnvironment
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Duration

/**
 * 조회 응답(ArticleResDto)의 Redis 캐시 읽기/쓰기.
 *
 * 쓰기는 조건 없이 덮어쓴다(단순 SET) — 레이스가 나도 TTL과 다음 Mongo backfill로 자연 치유되므로,
 * 원자성(Lua 등)을 확보할 실익이 없다고 판단했다.
 */
@Component
class ArticleCacheService(
    private val articleCacheRedisTemplate: RedisTemplate<String, ArticleResDto>,
    private val environment: ArticleCacheEnvironment,
) {
    fun get(articleId: Long): ArticleResDto? = articleCacheRedisTemplate.opsForValue().get(key(articleId))

    fun put(article: ArticleResDto) {
        articleCacheRedisTemplate
            .opsForValue()
            .set(key(article.id), article, Duration.ofMinutes(environment.ttlMinutes))
    }

    /**
     * 호출 시점이 트랜잭션 안이면 커밋 이후로 미뤄서 SET한다 — 커밋이 실패해 롤백되는데 Redis에는
     * "존재한 적 없는" 내용이 남는 상황을 막기 위함이다. 트랜잭션 밖이면 바로 SET한다.
     */
    fun putAfterCommit(article: ArticleResDto) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            put(article)
            return
        }
        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCommit() = put(article)
            },
        )
    }

    private fun key(articleId: Long) = "article:cache:$articleId"
}
