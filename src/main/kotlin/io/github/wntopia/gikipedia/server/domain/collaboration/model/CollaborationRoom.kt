package io.github.wntopia.gikipedia.server.domain.collaboration.model

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.ScheduledFuture

/**
 * article 하나에 대응하는 실시간 공동편집 방.
 *
 * Spring 빈이 아니라 [io.github.wntopia.gikipedia.server.domain.collaboration.service.CollaborationRoomRegistry]가
 * article별로 만들어 들고 있는 평범한 객체다. room 개수/트래픽이 크지 않으므로 모든 조작을 `@Synchronized`로
 * room 단위 직렬화하는 coarse lock으로 충분하다고 판단했다.
 */
class CollaborationRoom(
    val articleId: Long,
    private val objectMapper: ObjectMapper,
) {
    private val sessions = LinkedHashMap<String, WebSocketSession>()

    /** 가장 최근 `sync` 또는 `save` 메시지가 실어온 plain text — 안전망 저장의 원천. */
    var lastKnownPlainText: String? = null
        private set

    /** 위 내용을 마지막으로 보낸 편집자 라벨("학번 이름") — 안전망 저장 시 editor로 사용. */
    var lastEditorLabel: String? = null
        private set

    private var pendingLeaveSafetyNet: ScheduledFuture<*>? = null

    @Synchronized
    fun join(session: WebSocketSession) {
        sessions[session.id] = session
    }

    /** @return 이 호출로 인해 room이 비게 됐는지 여부 */
    @Synchronized
    fun leave(sessionId: String): Boolean {
        sessions.remove(sessionId)
        return sessions.isEmpty()
    }

    @Synchronized
    fun isEmpty(): Boolean = sessions.isEmpty()

    @Synchronized
    fun rememberSync(
        plainText: String,
        editor: String,
    ) {
        lastKnownPlainText = plainText
        lastEditorLabel = editor
    }

    @Synchronized
    fun schedulePendingSafetyNet(future: ScheduledFuture<*>) {
        pendingLeaveSafetyNet?.cancel(false)
        pendingLeaveSafetyNet = future
    }

    @Synchronized
    fun cancelPendingSafetyNet() {
        pendingLeaveSafetyNet?.cancel(false)
        pendingLeaveSafetyNet = null
    }

    /** 특정 세션 하나에만 보낸다(bootstrap, error 등 unicast). */
    @Synchronized
    fun sendTo(
        sessionId: String,
        message: CollaborationMessage,
    ) {
        sessions[sessionId]?.let { send(it, message) }
    }

    /** [exceptSessionId]를 제외한 room 내 모든 세션에 보낸다. */
    @Synchronized
    fun broadcast(
        message: CollaborationMessage,
        exceptSessionId: String? = null,
    ) {
        sessions.values
            .filter { it.id != exceptSessionId }
            .forEach { send(it, message) }
    }

    private fun send(
        session: WebSocketSession,
        message: CollaborationMessage,
    ) {
        if (!session.isOpen) return
        runCatching { session.sendMessage(TextMessage(objectMapper.writeValueAsString(message))) }
    }
}
