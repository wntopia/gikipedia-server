package io.github.wntopia.gikipedia.server.domain.collaboration.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.wntopia.gikipedia.server.domain.article.service.QueryArticleService
import io.github.wntopia.gikipedia.server.domain.collaboration.model.CollaborationMessage
import io.github.wntopia.gikipedia.server.domain.collaboration.model.CollaborationRoom
import io.github.wntopia.gikipedia.server.domain.collaboration.service.CollaborationCrdtStateStore
import io.github.wntopia.gikipedia.server.domain.collaboration.service.CollaborationRoomRegistry
import io.github.wntopia.gikipedia.server.domain.collaboration.service.SaveCollaborativeRevisionService
import io.github.wntopia.gikipedia.server.global.config.CollaborationEnvironment
import io.github.wntopia.gikipedia.server.global.security.session.AuthenticationReader
import org.slf4j.LoggerFactory
import org.springframework.scheduling.TaskScheduler
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.time.Instant
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

/**
 * article 하나당 하나의 room으로 묶이는 실시간 공동편집 WebSocket 핸들러.
 *
 * CRDT(Yjs) 바이트는 절대 디코딩하지 않고 opaque하게 릴레이만 한다 — "저장"에 필요한 plain text는 항상
 * 클라이언트가 `sync`/`save` 메시지에 실어 보내준다. 메시지 종류별 처리는
 * [CollaborationMessage]의 `type` 문서를 참고.
 */
@Component
class ArticleCollaborationWebSocketHandler(
    private val collaborationRoomRegistry: CollaborationRoomRegistry,
    private val collaborationCrdtStateStore: CollaborationCrdtStateStore,
    private val saveCollaborativeRevisionService: SaveCollaborativeRevisionService,
    private val queryArticleService: QueryArticleService,
    private val authenticationReader: AuthenticationReader,
    private val objectMapper: ObjectMapper,
    private val taskScheduler: TaskScheduler,
    private val environment: CollaborationEnvironment,
) : TextWebSocketHandler() {
    private val log = LoggerFactory.getLogger(javaClass)

    /** 세션 id → editor 라벨("학번 이름"). handshake 인증 결과를 연결 종료까지 들고 있기 위함. */
    private val editorLabelsBySessionId = ConcurrentHashMap<String, String>()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        val articleId = ArticleCollaborationPaths.articleIdOf(session.uri)
        if (articleId == null) {
            session.close(CloseStatus.BAD_DATA)
            return
        }

        // room에 join하기 전에 bootstrap 내용을 먼저 확정한다 — 존재하지 않는 article이면 여기서
        // queryArticleService가 404를 던지므로, room을 만들거나 세션을 등록하지 않고 그대로 접속을 거부한다.
        val bootstrap =
            runCatching { buildBootstrap(articleId) }
                .getOrElse {
                    log.warn("공동편집 접속 거부: article 조회 실패 (articleId={})", articleId, it)
                    session.close(CloseStatus.NOT_ACCEPTABLE)
                    return
                }

        editorLabelsBySessionId[session.id] = authenticationReader.getEditorLabel(session.attributes)
        val decoratedSession = ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MS, BUFFER_SIZE_LIMIT_BYTES)
        val room = collaborationRoomRegistry.joinOrCreate(articleId, decoratedSession)
        room.sendTo(session.id, bootstrap)
    }

    override fun handleTextMessage(
        session: WebSocketSession,
        message: TextMessage,
    ) {
        val articleId = ArticleCollaborationPaths.articleIdOf(session.uri) ?: return
        val room = collaborationRoomRegistry.findIfActive(articleId) ?: return

        val incoming =
            runCatching { objectMapper.readValue(message.payload, CollaborationMessage::class.java) }
                .getOrElse {
                    room.sendTo(
                        session.id,
                        CollaborationMessage(type = CollaborationMessage.TYPE_ERROR, message = "잘못된 메시지 형식입니다."),
                    )
                    return
                }

        // handleSync/handleSave가 던질 수 있는 예외(잘못된 base64, 저장 실패 등)를 여기서 한 곳에 모아
        // 처리한다 — 안 그러면 Spring의 기본 예외 핸들러가 세션 자체를 강제 종료시켜, 다른 참여자는 멀쩡한데
        // 이 사람만 재접속이 필요해진다.
        runCatching {
            when (incoming.type) {
                CollaborationMessage.TYPE_UPDATE, CollaborationMessage.TYPE_AWARENESS ->
                    room.broadcast(incoming, exceptSessionId = session.id)

                CollaborationMessage.TYPE_SYNC -> handleSync(room, articleId, session.id, incoming)

                CollaborationMessage.TYPE_SAVE -> handleSave(room, articleId, session.id, incoming)

                else -> log.debug("알 수 없는 협업 메시지 타입: {}", incoming.type)
            }
        }.onFailure { exception ->
            log.warn("협업 메시지 처리 실패 (articleId={}, type={})", articleId, incoming.type, exception)
            room.sendTo(
                session.id,
                CollaborationMessage(type = CollaborationMessage.TYPE_ERROR, message = "메시지 처리 중 오류가 발생했습니다."),
            )
        }
    }

    override fun afterConnectionClosed(
        session: WebSocketSession,
        closeStatus: CloseStatus,
    ) {
        editorLabelsBySessionId.remove(session.id)
        val articleId = ArticleCollaborationPaths.articleIdOf(session.uri) ?: return
        val room = collaborationRoomRegistry.findIfActive(articleId) ?: return

        if (room.leave(session.id)) {
            scheduleLeaveSafetyNet(room, articleId)
        }
    }

    /** Redis에 CRDT 상태가 있으면 그 바이트를, 없으면 현재 article의 plain text를 부트스트랩 메시지로 만든다.
     * article이 존재하지 않으면 [queryArticleService]가 404 예외를 던지며, 호출부가 그 실패를 접속 거부로 다룬다. */
    private fun buildBootstrap(articleId: Long): CollaborationMessage {
        val persistedState = collaborationCrdtStateStore.get(articleId)
        return if (persistedState != null) {
            CollaborationMessage(
                type = CollaborationMessage.TYPE_BOOTSTRAP,
                dataB64 = Base64.getEncoder().encodeToString(persistedState),
            )
        } else {
            val article = queryArticleService.execute(articleId)
            CollaborationMessage(
                type = CollaborationMessage.TYPE_BOOTSTRAP,
                content = article.content,
                imageUrl = article.imageUrl,
            )
        }
    }

    private fun handleSync(
        room: CollaborationRoom,
        articleId: Long,
        sessionId: String,
        incoming: CollaborationMessage,
    ) {
        val editor = editorLabelsBySessionId[sessionId] ?: return
        incoming.dataB64?.let { collaborationCrdtStateStore.put(articleId, Base64.getDecoder().decode(it)) }
        incoming.content?.let { room.rememberSync(it, editor) }
    }

    private fun handleSave(
        room: CollaborationRoom,
        articleId: Long,
        sessionId: String,
        incoming: CollaborationMessage,
    ) {
        val editor = editorLabelsBySessionId[sessionId] ?: return
        val plainText = incoming.content ?: return

        val revision = saveCollaborativeRevisionService.save(articleId, plainText, editor)
        room.rememberSync(plainText, editor)
        room.broadcast(CollaborationMessage(type = CollaborationMessage.TYPE_SAVED, revision = revision))
    }

    private fun scheduleLeaveSafetyNet(
        room: CollaborationRoom,
        articleId: Long,
    ) {
        val future =
            taskScheduler.schedule(
                { runLeaveSafetyNet(room, articleId) },
                Instant.now().plusSeconds(environment.leaveDebounceSeconds),
            )
        room.schedulePendingSafetyNet(future)
    }

    /**
     * 디바운스 만료 후 실행. 그 사이 재입장이 있었다면(레퍼런스가 registry의 room과 다르거나 더는 비어있지
     * 않으면) 아무것도 하지 않는다 — 재입장 레이스 방지.
     */
    private fun runLeaveSafetyNet(
        room: CollaborationRoom,
        articleId: Long,
    ) {
        if (collaborationRoomRegistry.findIfActive(articleId) !== room || !room.isEmpty()) {
            return
        }

        val plainText = room.lastKnownPlainText
        val editor = room.lastEditorLabel
        if (plainText != null && editor != null) {
            runCatching { saveCollaborativeRevisionService.save(articleId, plainText, editor) }
                .onFailure { log.warn("공동편집 안전망 저장 실패 (articleId={})", articleId, it) }
        }

        collaborationCrdtStateStore.delete(articleId)
        collaborationRoomRegistry.removeIfEmpty(room)
    }

    companion object {
        private const val SEND_TIME_LIMIT_MS = 5_000
        private const val BUFFER_SIZE_LIMIT_BYTES = 512 * 1024
    }
}
