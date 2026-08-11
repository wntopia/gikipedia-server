package io.github.wntopia.gikipedia.server.domain.collaboration.service

import io.github.wntopia.gikipedia.server.global.config.CollaborationEnvironment
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration

/** CRDT 상태의 키 네임스페이스와 TTL 적용이 올바른지 검증(ArticleCacheStoreTest와 같은 패턴). */
class CollaborationCrdtStateStoreTest {
    private val redisTemplate = mock<RedisTemplate<String, ByteArray>>()
    private val valueOps = mock<ValueOperations<String, ByteArray>>()
    private val store =
        CollaborationCrdtStateStore(
            redisTemplate,
            CollaborationEnvironment(crdtTtlHours = 6, leaveDebounceSeconds = 20),
        )

    init {
        whenever(redisTemplate.opsForValue()).thenReturn(valueOps)
    }

    @Test
    @DisplayName("articleId별 네임스페이스 키로 TTL과 함께 저장한다")
    fun putsWithNamespacedKeyAndTtl() {
        val state = byteArrayOf(1, 2, 3)

        store.put(1L, state)

        verify(valueOps).set(eq("collab:crdt-state:1"), eq(state), eq(Duration.ofHours(6)))
    }

    @Test
    @DisplayName("저장된 상태를 articleId 키로 조회한다")
    fun getsByArticleIdKey() {
        whenever(valueOps.get("collab:crdt-state:1")).thenReturn(byteArrayOf(9))

        val result = store.get(1L)

        assertThat(result).isEqualTo(byteArrayOf(9))
    }

    @Test
    @DisplayName("delete는 articleId 네임스페이스 키를 지운다")
    fun deletesByArticleIdKey() {
        store.delete(1L)

        verify(redisTemplate).delete("collab:crdt-state:1")
    }
}
