package io.github.wntopia.gikipedia.server.domain.collaboration.websocket

import io.github.wntopia.gikipedia.server.global.security.session.OAuthSessionAttributes
import org.springframework.http.HttpStatus
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor

/**
 * WS handshake 시점에 REST와 동일한 세션 인증을 강제한다.
 *
 * 부모 클래스가 `HttpSession` 속성을 `WebSocketSession.attributes`로 복사해오므로, 이후 핸들러는
 * `AuthenticationReader.getEditorLabel(attributes)`로 REST 경로(`getEditorLabel(session)`)와 동일한 인증
 * 정보를 읽을 수 있다. 인증 정보가 없거나 articleId를 경로에서 뽑을 수 없으면 handshake 자체를 거부한다.
 */
@Component
class ArticleCollaborationHandshakeInterceptor : HttpSessionHandshakeInterceptor() {
    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>,
    ): Boolean {
        if (!super.beforeHandshake(request, response, wsHandler, attributes)) {
            return false
        }
        if (attributes[OAuthSessionAttributes.DATAGSM_USER_INFO] == null) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED)
            return false
        }
        if (ArticleCollaborationPaths.articleIdOf(request.uri) == null) {
            response.setStatusCode(HttpStatus.BAD_REQUEST)
            return false
        }
        return true
    }
}
