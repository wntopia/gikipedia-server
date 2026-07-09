package io.github.wntopia.gikipedia.server.domain.auth.service.impl

import io.github.wntopia.gikipedia.server.domain.auth.dto.response.AuthStatusResDto
import io.github.wntopia.gikipedia.server.domain.auth.service.QueryAuthStatusService
import io.github.wntopia.gikipedia.server.global.security.session.AuthenticationReader
import jakarta.servlet.http.HttpSession
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import team.themoment.sdk.exception.ExpectedException

@Service
class QueryAuthStatusServiceImpl(
    private val authenticationReader: AuthenticationReader,
) : QueryAuthStatusService {
    override fun execute(session: HttpSession): AuthStatusResDto {
        val userInfo =
            authenticationReader.getCurrentUser(session)
                ?: throw ExpectedException("인증이 필요합니다.", HttpStatus.UNAUTHORIZED)
        return AuthStatusResDto(true, userInfo)
    }
}
