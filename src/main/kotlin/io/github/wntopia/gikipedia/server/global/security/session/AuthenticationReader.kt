package io.github.wntopia.gikipedia.server.global.security.session

import io.github.wntopia.gikipedia.server.domain.auth.dto.response.DataGsmUserInfoResDto
import jakarta.servlet.http.HttpSession
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import team.themoment.sdk.exception.ExpectedException

@Component
class AuthenticationReader {
    fun getCurrentUser(session: HttpSession): DataGsmUserInfoResDto? =
        session.getAttribute(OAuthSessionAttributes.DATAGSM_USER_INFO) as? DataGsmUserInfoResDto

    /**
     * WebSocket handshake 시점에 `HttpSessionHandshakeInterceptor`가 세션 속성을 그대로 복사해온
     * `WebSocketSession.attributes`에서 인증 정보를 읽기 위한 오버로드. `HttpSession`이 아닌 일반 Map이라
     * 별도 시그니처가 필요하다.
     */
    fun getCurrentUser(attributes: Map<String, Any?>): DataGsmUserInfoResDto? =
        attributes[OAuthSessionAttributes.DATAGSM_USER_INFO] as? DataGsmUserInfoResDto

    /** 수정자 식별 문자열("학번 이름", 예: "2412 홍길동")을 만든다. 인증 정보가 없으면 401. */
    fun getEditorLabel(session: HttpSession): String = editorLabelOf(getCurrentUser(session))

    fun getEditorLabel(attributes: Map<String, Any?>): String = editorLabelOf(getCurrentUser(attributes))

    private fun editorLabelOf(userInfo: DataGsmUserInfoResDto?): String {
        val student = userInfo?.student ?: throw ExpectedException("인증이 필요합니다.", HttpStatus.UNAUTHORIZED)
        return "${student.studentNumber ?: "?"} ${student.name ?: "?"}"
    }

    fun requireAdmin(session: HttpSession) {
        val role =
            getCurrentUser(session)?.role
                ?: throw ExpectedException("인증이 필요합니다.", HttpStatus.UNAUTHORIZED)
        if (role !in ADMIN_ROLES) {
            throw ExpectedException("관리자만 접근할 수 있습니다.", HttpStatus.FORBIDDEN)
        }
    }

    companion object {
        private val ADMIN_ROLES = setOf("ADMIN", "ROOT")
    }
}
