package io.github.wntopia.gikipedia.server.domain.collaboration.model

import io.github.wntopia.gikipedia.server.global.security.session.EditorIdentity
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.web.socket.WebSocketSession
import tools.jackson.module.kotlin.jacksonObjectMapper

/** 릴레이(broadcast/sendTo) 대상 선택과, 한 세션의 전송 실패가 다른 세션·호출자에게 전파되지 않는지 검증. */
class CollaborationRoomTest {
    private val room = CollaborationRoom(1L, jacksonObjectMapper())

    private fun fakeSession(id: String): WebSocketSession {
        val session = mock<WebSocketSession>()
        whenever(session.id).thenReturn(id)
        whenever(session.isOpen).thenReturn(true)
        return session
    }

    @Test
    @DisplayName("broadcast는 exceptSessionId를 제외한 나머지 세션에만 보낸다")
    fun broadcastExcludesGivenSession() {
        val a = fakeSession("a")
        val b = fakeSession("b")
        room.join(a)
        room.join(b)

        room.broadcast(CollaborationMessage(type = "update"), exceptSessionId = "a")

        verify(a, never()).sendMessage(any())
        verify(b).sendMessage(any())
    }

    @Test
    @DisplayName("sendTo는 지정한 세션에게만 보낸다")
    fun sendToTargetsOnlyGivenSession() {
        val a = fakeSession("a")
        val b = fakeSession("b")
        room.join(a)
        room.join(b)

        room.sendTo("a", CollaborationMessage(type = "bootstrap"))

        verify(a).sendMessage(any())
        verify(b, never()).sendMessage(any())
    }

    @Test
    @DisplayName("한 세션의 sendMessage가 예외를 던져도 broadcast 호출자에게 전파되지 않고, 다른 세션은 정상 수신한다")
    fun sendFailureOnOneSessionDoesNotAffectOthers() {
        val broken = fakeSession("broken")
        val healthy = fakeSession("healthy")
        whenever(broken.sendMessage(any())).thenThrow(RuntimeException("연결 끊김"))
        room.join(broken)
        room.join(healthy)

        assertThatCode { room.broadcast(CollaborationMessage(type = "update")) }.doesNotThrowAnyException()

        verify(healthy).sendMessage(any())
    }

    @Test
    @DisplayName("닫힌 세션에는 전송을 시도하지 않는다")
    fun doesNotSendToClosedSession() {
        val closed = fakeSession("closed")
        whenever(closed.isOpen).thenReturn(false)
        room.join(closed)

        room.broadcast(CollaborationMessage(type = "update"))

        verify(closed, never()).sendMessage(any())
    }

    @Test
    @DisplayName("접속한 편집자가 현재 편집자로 나오고, 나가면 빠진다")
    fun tracksCurrentEditors() {
        room.join(fakeSession("a"), EditorIdentity(1L, "2412 홍길동", "홍길동"))
        room.join(fakeSession("b"), EditorIdentity(2L, "2413 김철수", "김철수"))

        assertThat(room.currentEditors().map { it.name }).containsExactly("홍길동", "김철수")

        room.leave("a")
        assertThat(room.currentEditors().map { it.name }).containsExactly("김철수")

        room.leave("b")
        assertThat(room.currentEditors()).isEmpty()
    }

    @Test
    @DisplayName("같은 사용자가 여러 탭으로 접속해도 한 명으로 센다")
    fun countsSameUserOnce() {
        val editor = EditorIdentity(1L, "2412 홍길동", "홍길동")
        room.join(fakeSession("a"), editor)
        room.join(fakeSession("b"), editor)

        assertThat(room.currentEditors()).hasSize(1)
        assertThat(room.leave("a")).isFalse()
        assertThat(room.currentEditors()).hasSize(1)
    }
}
