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

    /** 수정자 식별 문자열("학번 이름", 예: "2412 홍길동")을 만든다. 인증 정보가 없으면 401. */
    fun getEditorLabel(session: HttpSession): String {
        val student =
            getCurrentUser(session)?.student
                ?: throw ExpectedException("인증이 필요합니다.", HttpStatus.UNAUTHORIZED)
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
