package io.github.wntopia.gikipedia.server.domain.collaboration.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** article 단위 room 생성/조회/제거의 레퍼런스 동일성·재입장 레이스 방지 로직을 검증. */
class CollaborationRoomRegistryTest {
    private val registry = CollaborationRoomRegistry(jacksonObjectMapper())

    @Test
    @DisplayName("같은 articleId로 여러 번 호출해도 같은 room 인스턴스를 반환한다")
    fun sameArticleIdReturnsSameRoom() {
        val first = registry.getOrCreate(1L)
        val second = registry.getOrCreate(1L)

        assertThat(first).isSameAs(second)
    }

    @Test
    @DisplayName("articleId가 다르면 서로 다른 room을 만든다")
    fun differentArticleIdCreatesDifferentRoom() {
        val room1 = registry.getOrCreate(1L)
        val room2 = registry.getOrCreate(2L)

        assertThat(room1).isNotSameAs(room2)
    }

    @Test
    @DisplayName("아직 만들어지지 않은 room은 findIfActive가 null을 반환한다")
    fun findIfActiveReturnsNullForUnknownRoom() {
        assertThat(registry.findIfActive(99L)).isNull()
    }

    @Test
    @DisplayName("removeIfEmpty는 넘겨준 room이 registry의 현재 room과 같으면 제거한다")
    fun removesMatchingRoom() {
        val room = registry.getOrCreate(1L)

        registry.removeIfEmpty(room)

        assertThat(registry.findIfActive(1L)).isNull()
    }

    @Test
    @DisplayName("그 사이 재입장으로 room이 교체됐다면(레퍼런스 불일치) 오래된 레퍼런스로는 제거되지 않는다")
    fun doesNotRemoveStaleRoomReference() {
        val staleRoom = registry.getOrCreate(1L)
        registry.removeIfEmpty(staleRoom)
        val newRoom = registry.getOrCreate(1L)

        registry.removeIfEmpty(staleRoom)

        assertThat(registry.findIfActive(1L)).isSameAs(newRoom)
    }
}
