package io.github.wntopia.gikipedia.server.domain.collaboration.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.web.socket.WebSocketSession

/** article 단위 room 생성/조회/제거의 레퍼런스 동일성·재입장 레이스 방지 로직을 검증. */
class CollaborationRoomRegistryTest {
    private val registry = CollaborationRoomRegistry(jacksonObjectMapper())

    private fun fakeSession(id: String): WebSocketSession {
        val session = mock<WebSocketSession>()
        whenever(session.id).thenReturn(id)
        whenever(session.isOpen).thenReturn(true)
        return session
    }

    @Test
    @DisplayName("같은 articleId로 여러 번 호출해도 같은 room 인스턴스를 반환한다")
    fun sameArticleIdReturnsSameRoom() {
        val first = registry.joinOrCreate(1L, fakeSession("a"))
        val second = registry.joinOrCreate(1L, fakeSession("b"))

        assertThat(first).isSameAs(second)
    }

    @Test
    @DisplayName("articleId가 다르면 서로 다른 room을 만든다")
    fun differentArticleIdCreatesDifferentRoom() {
        val room1 = registry.joinOrCreate(1L, fakeSession("a"))
        val room2 = registry.joinOrCreate(2L, fakeSession("b"))

        assertThat(room1).isNotSameAs(room2)
    }

    @Test
    @DisplayName("아직 만들어지지 않은 room은 findIfActive가 null을 반환한다")
    fun findIfActiveReturnsNullForUnknownRoom() {
        assertThat(registry.findIfActive(99L)).isNull()
    }

    @Test
    @DisplayName("removeIfEmpty는 넘겨준 room이 registry의 현재 room이고 실제로 비어있으면 제거한다")
    fun removesMatchingEmptyRoom() {
        val session = fakeSession("a")
        val room = registry.joinOrCreate(1L, session)
        room.leave("a")

        registry.removeIfEmpty(room)

        assertThat(registry.findIfActive(1L)).isNull()
    }

    @Test
    @DisplayName("그 사이 재입장으로 room이 교체됐다면(레퍼런스 불일치) 오래된 레퍼런스로는 제거되지 않는다")
    fun doesNotRemoveStaleRoomReference() {
        val staleRoom = registry.joinOrCreate(1L, fakeSession("a"))
        staleRoom.leave("a")
        registry.removeIfEmpty(staleRoom)
        val newRoom = registry.joinOrCreate(1L, fakeSession("b"))

        registry.removeIfEmpty(staleRoom)

        assertThat(registry.findIfActive(1L)).isSameAs(newRoom)
    }

    @Test
    @DisplayName("레퍼런스는 같아도 그 사이 재입장으로 다시 채워졌으면(비어있지 않으면) 제거하지 않는다")
    fun doesNotRemoveRoomThatWasRejoined() {
        val room = registry.joinOrCreate(1L, fakeSession("a"))
        room.leave("a")
        // 안전망 스레드가 isEmpty()를 확인한 뒤, removeIfEmpty를 호출하기 직전 재접속이 끼어든 상황을 흉내낸다.
        registry.joinOrCreate(1L, fakeSession("b"))

        registry.removeIfEmpty(room)

        assertThat(registry.findIfActive(1L)).isSameAs(room)
    }
}
