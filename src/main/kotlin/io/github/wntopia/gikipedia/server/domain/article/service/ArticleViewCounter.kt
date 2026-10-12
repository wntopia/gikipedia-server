package io.github.wntopia.gikipedia.server.domain.article.service

import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.global.config.ArticleViewEnvironment
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * 문서 조회수 집계. 읽기 경로(Redis → Mongo)가 MySQL을 건드리지 않도록 누적 조회수를 Redis에도 둔다.
 *
 * - `article:views:total`(hash): 문서별 누적 조회수. 비어 있으면 MySQL 값(+아직 반영 안 된 증가분)으로 채운다.
 * - `article:views:pending`(hash): MySQL에 아직 반영하지 않은 증가분. [ArticleViewFlushScheduler]가 주기적으로 비운다.
 * - `article:views:seen:{문서}:{조회자}`: 중복 집계를 막는 키. TTL 동안 같은 조회자의 재조회는 세지 않는다.
 */
@Component
class ArticleViewCounter(
    private val redis: StringRedisTemplate,
    private val articleRepository: ArticleRepository,
    private val environment: ArticleViewEnvironment,
) {
    /**
     * 조회를 기록하고 현재 누적 조회수를 돌려준다. 같은 조회자가 중복 집계 시간 안에 다시 열면 증가시키지 않는다.
     * 조회수 때문에 문서 조회가 실패하면 안 되므로 Redis 오류는 호출 측에서 처리한다.
     */
    fun recordView(
        articleId: Long,
        viewerKey: String,
    ): Long {
        val total = ensureTotal(articleId)
        val firstView =
            redis
                .opsForValue()
                .setIfAbsent(seenKey(articleId, viewerKey), "1", Duration.ofMinutes(environment.dedupeMinutes)) == true
        if (!firstView) return total

        redis.opsForHash<String, String>().increment(PENDING_KEY, articleId.toString(), 1)
        return redis.opsForHash<String, String>().increment(TOTAL_KEY, articleId.toString(), 1)
    }

    /** 증가분 반영을 위해 pending을 가져가고 비운다. 반환값은 문서 id → 증가분. */
    fun drainPending(): Map<Long, Long> {
        if (redis.hasKey(PROCESSING_KEY) != true) {
            if (redis.hasKey(PENDING_KEY) != true) return emptyMap()
            redis.rename(PENDING_KEY, PROCESSING_KEY)
        }
        return redis
            .opsForHash<String, String>()
            .entries(PROCESSING_KEY)
            .mapNotNull { (id, delta) -> id.toLongOrNull()?.let { it to delta.toLong() } }
            .toMap()
    }

    /** [drainPending]으로 가져간 증가분을 MySQL에 반영한 뒤 호출한다. */
    fun confirmDrained() {
        redis.delete(PROCESSING_KEY)
    }

    private fun ensureTotal(articleId: Long): Long {
        val hash = redis.opsForHash<String, String>()
        val field = articleId.toString()
        hash.get(TOTAL_KEY, field)?.let { return it.toLong() }

        val pending = hash.get(PENDING_KEY, field)?.toLong() ?: 0
        val seeded = (articleRepository.findViewCountById(articleId) ?: 0) + pending
        hash.putIfAbsent(TOTAL_KEY, field, seeded.toString())
        return hash.get(TOTAL_KEY, field)?.toLong() ?: seeded
    }

    private fun seenKey(
        articleId: Long,
        viewerKey: String,
    ) = "article:views:seen:$articleId:$viewerKey"

    companion object {
        private const val TOTAL_KEY = "article:views:total"
        private const val PENDING_KEY = "article:views:pending"
        private const val PROCESSING_KEY = "article:views:processing"
    }
}
