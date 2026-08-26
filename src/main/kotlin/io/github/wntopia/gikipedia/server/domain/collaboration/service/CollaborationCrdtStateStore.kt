package io.github.wntopia.gikipedia.server.domain.collaboration.service

import io.github.wntopia.gikipedia.server.global.config.CollaborationEnvironment
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * 편집 중인 CRDT(Yjs) 문서의 바이너리 상태(opaque byte)를 Redis에 읽고 쓴다.
 *
 * `ArticleCacheStore`와 마찬가지로 쓰기는 조건 없이 덮어쓴다 — 각 클라이언트가 독립적으로 디바운스해서
 * 보내는 전체 스냅샷이라 원자성을 확보할 실익이 없다.
 */
@Component
class CollaborationCrdtStateStore(
    private val collaborationCrdtRedisTemplate: RedisTemplate<String, ByteArray>,
    private val environment: CollaborationEnvironment,
) {
    fun get(articleId: Long): ByteArray? = collaborationCrdtRedisTemplate.opsForValue().get(key(articleId))

    fun put(
        articleId: Long,
        state: ByteArray,
    ) {
        collaborationCrdtRedisTemplate
            .opsForValue()
            .set(key(articleId), state, Duration.ofHours(environment.crdtTtlHours))
    }

    fun delete(articleId: Long) {
        collaborationCrdtRedisTemplate.delete(key(articleId))
    }

    private fun key(articleId: Long) = "collab:crdt-state:$articleId"
}
