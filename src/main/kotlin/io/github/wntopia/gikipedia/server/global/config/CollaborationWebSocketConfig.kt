package io.github.wntopia.gikipedia.server.global.config

import io.github.wntopia.gikipedia.server.domain.collaboration.websocket.ArticleCollaborationHandshakeInterceptor
import io.github.wntopia.gikipedia.server.domain.collaboration.websocket.ArticleCollaborationWebSocketHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean

/**
 * article 실시간 공동편집 WebSocket 엔드포인트 등록.
 *
 * WS handshake는 Spring MVC의 CORS 필터를 타지 않으므로, REST에 쓰는 [CorsEnvironment]의 허용 origin을
 * `setAllowedOriginPatterns`에 그대로 재사용해 두 경로의 CORS 정책이 어긋나지 않게 한다.
 */
@Configuration
@EnableWebSocket
class CollaborationWebSocketConfig(
    private val articleCollaborationWebSocketHandler: ArticleCollaborationWebSocketHandler,
    private val handshakeInterceptor: ArticleCollaborationHandshakeInterceptor,
    private val corsEnvironment: CorsEnvironment,
) : WebSocketConfigurer {
    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry
            .addHandler(articleCollaborationWebSocketHandler, "/ws/articles/*/collaboration")
            .addInterceptors(handshakeInterceptor)
            .setAllowedOriginPatterns(*corsEnvironment.allowedOrigins.toTypedArray())
    }

    /** 기본 버퍼(8KB)는 부트스트랩 시 보내는 base64 CRDT 스냅샷엔 부족할 수 있어 넉넉히 늘린다. */
    @Bean
    fun collaborationWebSocketContainer(): ServletServerContainerFactoryBean =
        ServletServerContainerFactoryBean().apply {
            setMaxTextMessageBufferSize(MAX_MESSAGE_BUFFER_SIZE_BYTES)
            setMaxBinaryMessageBufferSize(MAX_MESSAGE_BUFFER_SIZE_BYTES)
        }

    companion object {
        private const val MAX_MESSAGE_BUFFER_SIZE_BYTES = 512 * 1024
    }
}
