package io.github.wntopia.gikipedia.server.domain.collaboration.websocket

import io.github.wntopia.gikipedia.server.domain.article.dto.response.ArticleResDto
import io.github.wntopia.gikipedia.server.domain.article.service.QueryArticleService
import io.github.wntopia.gikipedia.server.domain.collaboration.model.CollaborationMessage
import io.github.wntopia.gikipedia.server.domain.collaboration.service.CollaborationCrdtStateStore
import io.github.wntopia.gikipedia.server.domain.collaboration.service.CollaborationRoomRegistry
import io.github.wntopia.gikipedia.server.domain.collaboration.service.SaveCollaborativeRevisionService
import io.github.wntopia.gikipedia.server.global.config.CollaborationEnvironment
import io.github.wntopia.gikipedia.server.global.security.session.AuthenticationReader
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.scheduling.TaskScheduler
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.net.URI
import java.time.Instant
import java.util.concurrent.ScheduledFuture

/**
 * 실제 소켓 없이 fake `WebSocketSession`(mock) 여러 개를 같은 핸들러 인스턴스에 접속시켜 릴레이·room
 * 격리·presence·저장·안전망 로직을 검증한다. `TaskScheduler`도 mock으로 주입해 디바운스를 실제 대기 없이
 * 결정론적으로 테스트한다.
 *
 * `ConcurrentWebSocketSessionDecorator`는 락이 경합 없이 즉시 잡히면 delegate.sendMessage()를 동기적으로
 * 호출하므로, 이 테스트처럼 단일 스레드에서 순차 호출하면 fake 세션의 sendMessage가 그 자리에서 호출된다.
 */
class ArticleCollaborationWebSocketHandlerTest {
    private val collaborationRoomRegistry = CollaborationRoomRegistry(jacksonObjectMapper())
    private val collaborationCrdtStateStore = mock<CollaborationCrdtStateStore>()
    private val saveCollaborativeRevisionService = mock<SaveCollaborativeRevisionService>()
    private val queryArticleService = mock<QueryArticleService>()
    private val authenticationReader = mock<AuthenticationReader>()
    private val objectMapper = jacksonObjectMapper()
    private val taskScheduler = mock<TaskScheduler>()
    private val environment = CollaborationEnvironment(crdtTtlHours = 6, leaveDebounceSeconds = 20)

    private val handler =
        ArticleCollaborationWebSocketHandler(
            collaborationRoomRegistry,
            collaborationCrdtStateStore,
            saveCollaborativeRevisionService,
            queryArticleService,
            authenticationReader,
            objectMapper,
            taskScheduler,
            environment,
        )

    @BeforeEach
    fun setUp() {
        whenever(queryArticleService.execute(any())).thenReturn(
            ArticleResDto(1L, "제목", "본문", null, Instant.now(), Instant.now()),
        )
        whenever(collaborationCrdtStateStore.get(any())).thenReturn(null)
    }

    /** 세션마다 attributes에 서로 다른 값을 넣어, 빈 맵끼리 `equals()`로 겹쳐 stub이 뒤섞이는 걸 막는다. */
    private fun fakeSession(
        id: String,
        articleId: Long,
        editor: String,
    ): WebSocketSession {
        val session = mock<WebSocketSession>()
        val attributes = mutableMapOf<String, Any>("sessionId" to id)
        whenever(session.id).thenReturn(id)
        whenever(session.uri).thenReturn(URI.create("ws://localhost/ws/articles/$articleId/collaboration"))
        whenever(session.attributes).thenReturn(attributes)
        whenever(session.isOpen).thenReturn(true)
        whenever(authenticationReader.getEditorLabel(attributes)).thenReturn(editor)
        return session
    }

    private fun messagesSentTo(session: WebSocketSession): List<CollaborationMessage> {
        val captor = argumentCaptor<TextMessage>()
        verify(session, atLeast(0)).sendMessage(captor.capture())
        return captor.allValues.map { objectMapper.readValue(it.payload, CollaborationMessage::class.java) }
    }

    @Test
    @DisplayName("update/awareness는 발신자를 제외한 room의 다른 세션에만 릴레이된다")
    fun relaysUpdateExceptSender() {
        val sessionA = fakeSession("a", 1L, "2412 홍길동")
        val sessionB = fakeSession("b", 1L, "2413 김철수")
        handler.afterConnectionEstablished(sessionA)
        handler.afterConnectionEstablished(sessionB)

        val update = CollaborationMessage(type = CollaborationMessage.TYPE_UPDATE, dataB64 = "AAA")
        handler.handleMessage(sessionA, TextMessage(objectMapper.writeValueAsString(update)))

        assertThat(messagesSentTo(sessionB)).anyMatch {
            it.type == CollaborationMessage.TYPE_UPDATE &&
                it.dataB64 == "AAA"
        }
        assertThat(messagesSentTo(sessionA)).noneMatch { it.type == CollaborationMessage.TYPE_UPDATE }
    }

    @Test
    @DisplayName("articleId가 다른 room끼리는 릴레이가 격리된다")
    fun isolatesDifferentArticleRooms() {
        val sessionA = fakeSession("a", 1L, "2412 홍길동")
        val sessionC = fakeSession("c", 2L, "2414 이영희")
        handler.afterConnectionEstablished(sessionA)
        handler.afterConnectionEstablished(sessionC)

        val update = CollaborationMessage(type = CollaborationMessage.TYPE_UPDATE, dataB64 = "AAA")
        handler.handleMessage(sessionA, TextMessage(objectMapper.writeValueAsString(update)))

        assertThat(messagesSentTo(sessionC)).noneMatch { it.type == CollaborationMessage.TYPE_UPDATE }
    }

    @Test
    @DisplayName("Redis에 저장된 CRDT 상태가 있으면 접속 직후 그 바이트를 bootstrap으로 보낸다")
    fun bootstrapsFromRedisWhenAvailable() {
        whenever(collaborationCrdtStateStore.get(1L)).thenReturn(byteArrayOf(1, 2, 3))
        val session = fakeSession("a", 1L, "2412 홍길동")

        handler.afterConnectionEstablished(session)

        val bootstrap = messagesSentTo(session).first { it.type == CollaborationMessage.TYPE_BOOTSTRAP }
        assertThat(bootstrap.dataB64).isNotNull()
        assertThat(bootstrap.content).isNull()
    }

    @Test
    @DisplayName("Redis에 저장된 CRDT 상태가 없으면 현재 article의 plain text로 bootstrap한다")
    fun bootstrapsFromArticleWhenRedisEmpty() {
        val session = fakeSession("a", 1L, "2412 홍길동")

        handler.afterConnectionEstablished(session)

        val bootstrap = messagesSentTo(session).first { it.type == CollaborationMessage.TYPE_BOOTSTRAP }
        assertThat(bootstrap.content).isEqualTo("본문")
        assertThat(bootstrap.dataB64).isNull()
    }

    @Test
    @DisplayName("save 메시지를 받으면 저장 서비스를 호출하고 room 전체에 saved를 브로드캐스트한다")
    fun savesAndBroadcastsSaved() {
        whenever(saveCollaborativeRevisionService.save(1L, "새 내용", "2412 홍길동")).thenReturn(5)
        val sessionA = fakeSession("a", 1L, "2412 홍길동")
        val sessionB = fakeSession("b", 1L, "2413 김철수")
        handler.afterConnectionEstablished(sessionA)
        handler.afterConnectionEstablished(sessionB)

        val save = CollaborationMessage(type = CollaborationMessage.TYPE_SAVE, content = "새 내용")
        handler.handleMessage(sessionA, TextMessage(objectMapper.writeValueAsString(save)))

        verify(saveCollaborativeRevisionService).save(1L, "새 내용", "2412 홍길동")
        assertThat(messagesSentTo(sessionA)).anyMatch { it.type == CollaborationMessage.TYPE_SAVED && it.revision == 5 }
        assertThat(messagesSentTo(sessionB)).anyMatch { it.type == CollaborationMessage.TYPE_SAVED && it.revision == 5 }
    }

    @Test
    @DisplayName("마지막 참여자가 나가면 디바운스 안전망 저장이 예약되고, 만료 시 저장 후 room/Redis를 정리한다")
    fun schedulesLeaveSafetyNetAndCleansUp() {
        val runnableCaptor = argumentCaptor<Runnable>()
        val scheduledFuture = mock<ScheduledFuture<*>>()
        whenever(taskScheduler.schedule(runnableCaptor.capture(), any<Instant>())).thenReturn(scheduledFuture)

        val session = fakeSession("a", 1L, "2412 홍길동")
        handler.afterConnectionEstablished(session)
        val sync = CollaborationMessage(type = CollaborationMessage.TYPE_SYNC, content = "마지막 내용")
        handler.handleMessage(session, TextMessage(objectMapper.writeValueAsString(sync)))

        handler.afterConnectionClosed(session, CloseStatus.NORMAL)
        verify(taskScheduler).schedule(any<Runnable>(), any<Instant>())

        // 실제 벽시계 대기 없이, 예약된 안전망 작업을 결정론적으로 직접 실행한다.
        runnableCaptor.firstValue.run()

        verify(saveCollaborativeRevisionService).save(1L, "마지막 내용", "2412 홍길동")
        verify(collaborationCrdtStateStore).delete(1L)
        assertThat(collaborationRoomRegistry.findIfActive(1L)).isNull()
    }

    @Test
    @DisplayName("안전망 예약 이후 같은 room에 재입장하면 예약된 작업을 취소한다")
    fun cancelsPendingSafetyNetOnRejoin() {
        val scheduledFuture = mock<ScheduledFuture<*>>()
        whenever(taskScheduler.schedule(any<Runnable>(), any<Instant>())).thenReturn(scheduledFuture)

        val sessionA = fakeSession("a", 1L, "2412 홍길동")
        handler.afterConnectionEstablished(sessionA)
        handler.afterConnectionClosed(sessionA, CloseStatus.NORMAL)
        verify(taskScheduler).schedule(any<Runnable>(), any<Instant>())

        val sessionB = fakeSession("b", 1L, "2413 김철수")
        handler.afterConnectionEstablished(sessionB)

        verify(scheduledFuture).cancel(false)
    }

    @Test
    @DisplayName("존재하지 않는 article로 접속하면 room을 만들거나 세션을 등록하지 않고 접속을 거부한다")
    fun rejectsConnectionForNonexistentArticle() {
        whenever(queryArticleService.execute(1L)).thenThrow(RuntimeException("존재하지 않는 게시글입니다."))
        val session = fakeSession("a", 1L, "2412 홍길동")

        handler.afterConnectionEstablished(session)

        verify(session).close(CloseStatus.NOT_ACCEPTABLE)
        assertThat(collaborationRoomRegistry.findIfActive(1L)).isNull()
    }

    @Test
    @DisplayName("sync의 dataB64가 깨진 base64여도 세션을 강제 종료하지 않고 발신자에게 TYPE_ERROR를 보낸다")
    fun sendsErrorInsteadOfCrashingOnMalformedSyncPayload() {
        val session = fakeSession("a", 1L, "2412 홍길동")
        handler.afterConnectionEstablished(session)

        val brokenSync = CollaborationMessage(type = CollaborationMessage.TYPE_SYNC, dataB64 = "!!!not-base64!!!")
        handler.handleMessage(session, TextMessage(objectMapper.writeValueAsString(brokenSync)))

        assertThat(messagesSentTo(session)).anyMatch { it.type == CollaborationMessage.TYPE_ERROR }
        verify(session, org.mockito.kotlin.never()).close(any<CloseStatus>())
    }

    @Test
    @DisplayName("save 처리 중 예외가 나도 세션을 강제 종료하지 않고 발신자에게 TYPE_ERROR를 보낸다")
    fun sendsErrorInsteadOfCrashingWhenSaveFails() {
        whenever(saveCollaborativeRevisionService.save(any(), any(), any())).thenThrow(RuntimeException("저장 실패"))
        val session = fakeSession("a", 1L, "2412 홍길동")
        handler.afterConnectionEstablished(session)

        val save = CollaborationMessage(type = CollaborationMessage.TYPE_SAVE, content = "새 내용")
        handler.handleMessage(session, TextMessage(objectMapper.writeValueAsString(save)))

        assertThat(messagesSentTo(session)).anyMatch { it.type == CollaborationMessage.TYPE_ERROR }
        verify(session, org.mockito.kotlin.never()).close(any<CloseStatus>())
    }
}
