package io.github.wntopia.gikipedia.server.global.security.session

import io.github.wntopia.gikipedia.server.domain.auth.dto.response.DataGsmUserInfoResDto
import jakarta.servlet.http.HttpSession
import org.springframework.stereotype.Component

@Component
class AuthenticationReader {
    fun getCurrentUser(session: HttpSession): DataGsmUserInfoResDto? =
        session.getAttribute(OAuthSessionAttributes.DATAGSM_USER_INFO) as? DataGsmUserInfoResDto
}
