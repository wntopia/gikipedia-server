package io.github.wntopia.gikipedia.server.domain.collaboration.event

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.wntopia.gikipedia.server.domain.collaboration.model.CollaborationMessage
import io.github.wntopia.gikipedia.server.domain.collaboration.model.CollaborationRoom
import io.github.wntopia.gikipedia.server.domain.collaboration.service.CollaborationRoomRegistry
import io.github.wntopia.gikipedia.server.domain.history.event.ArticleUpdatedEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import java.time.Instant

/**
 * PUT/이미지 업로드가 발행하는 [ArticleUpdatedEvent]가 활성 공동편집 room에 실시간으로 전달되는지 검증.
 * `CollaborationRoom`은 Spring 빈이 아닌 평범한 final 클래스라 mock 대신 실제 인스턴스 + fake 세션으로
 * 검증한다.
 */
class CollaborationExternalUpdateListenerTest {
    private val objectMapper = jacksonObjectMapper()
    private val collaborationRoomRegistry = mock<CollaborationRoomRegistry>()
    private val listener = CollaborationExternalUpdateListener(collaborationRoomRegistry)

    private val event =
        ArticleUpdatedEvent(
            articleId = 1L,
            revision = 3,
            title = "제목",
            content = "본문",
            imageUrl = "new.png",
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )

    @Test
    @DisplayName("활성 room이 있으면 접속 중인 세션에 external-update를 브로드캐스트한다")
    fun broadcastsToActiveRoom() {
        val room = CollaborationRoom(1L, objectMapper)
        val session = mock<WebSocketSession>()
        whenever(session.id).thenReturn("a")
        whenever(session.isOpen).thenReturn(true)
        room.join(session)
        whenever(collaborationRoomRegistry.findIfActive(1L)).thenReturn(room)

        listener.onArticleUpdated(event)

        val captor = argumentCaptor<TextMessage>()
        verify(session, atLeast(1)).sendMessage(captor.capture())
        val received = objectMapper.readValue(captor.firstValue.payload, CollaborationMessage::class.java)
        assertThat(received.type).isEqualTo(CollaborationMessage.TYPE_EXTERNAL_UPDATE)
        assertThat(received.content).isEqualTo("본문")
        assertThat(received.imageUrl).isEqualTo("new.png")
    }

    @Test
    @DisplayName("활성 room이 없으면 아무 것도 하지 않는다")
    fun doesNothingWithoutActiveRoom() {
        whenever(collaborationRoomRegistry.findIfActive(1L)).thenReturn(null)

        listener.onArticleUpdated(event)
    }
}
