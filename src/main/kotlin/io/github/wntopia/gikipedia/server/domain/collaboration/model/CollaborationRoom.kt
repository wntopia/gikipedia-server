package io.github.wntopia.gikipedia.server.domain.collaboration.model

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
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

    /**
     * 특정 세션 하나에만 보낸다(bootstrap, error 등 unicast).
     *
     * 대상 세션을 조회하는 동안만 잠그고, 실제 blocking I/O인 `sendMessage`는 잠금 밖에서 수행한다 —
     * 한 세션의 느린 전송이 같은 room의 다른 join/leave/broadcast까지 붙잡는 것을 막기 위함이다.
     */
    fun sendTo(
        sessionId: String,
        message: CollaborationMessage,
    ) {
        val target = synchronized(this) { sessions[sessionId] } ?: return
        send(target, message)
    }

    /** [exceptSessionId]를 제외한 room 내 모든 세션에 보낸다. sendTo와 같은 이유로 전송은 잠금 밖에서 한다. */
    fun broadcast(
        message: CollaborationMessage,
        exceptSessionId: String? = null,
    ) {
        val targets = synchronized(this) { sessions.values.filter { it.id != exceptSessionId } }
        targets.forEach { send(it, message) }
    }

    private fun send(
        session: WebSocketSession,
        message: CollaborationMessage,
    ) {
        if (!session.isOpen) return
        runCatching { session.sendMessage(TextMessage(objectMapper.writeValueAsString(message))) }
            .onFailure { log.warn("공동편집 메시지 전송 실패 (sessionId={}, type={})", session.id, message.type, it) }
    }

    companion object {
        private val log = LoggerFactory.getLogger(CollaborationRoom::class.java)
    }
}
