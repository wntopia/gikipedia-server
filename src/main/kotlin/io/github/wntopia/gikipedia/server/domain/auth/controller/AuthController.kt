package io.github.wntopia.gikipedia.server.domain.auth.controller

import io.github.wntopia.gikipedia.server.domain.auth.dto.request.DataGsmOAuthCallbackReqDto
import io.github.wntopia.gikipedia.server.domain.auth.dto.response.AuthStatusResDto
import io.github.wntopia.gikipedia.server.domain.auth.service.QueryAuthStatusService
import io.github.wntopia.gikipedia.server.domain.auth.service.QueryAuthorizationUriService
import io.github.wntopia.gikipedia.server.domain.auth.service.SigninService
import io.github.wntopia.gikipedia.server.domain.auth.service.SignoutService
import jakarta.servlet.http.HttpSession
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import team.themoment.sdk.response.CommonApiResponse

@RestController
class AuthController(
    private val queryAuthorizationUriService: QueryAuthorizationUriService,
    private val signinService: SigninService,
    private val queryAuthStatusService: QueryAuthStatusService,
    private val signoutService: SignoutService,
) {
    @GetMapping("/api/v1/oauth/datagsm/signin")
    fun signin(session: HttpSession): ResponseEntity<Void> = queryAuthorizationUriService.execute(session)

    @GetMapping("/api/v1/oauth/datagsm/callback")
    fun callback(
        @ModelAttribute reqDto: DataGsmOAuthCallbackReqDto,
        session: HttpSession,
    ): ResponseEntity<Void> = signinService.execute(reqDto, session)

    @GetMapping("/api/v1/auth/me")
    fun me(session: HttpSession): AuthStatusResDto = queryAuthStatusService.execute(session)

    @PostMapping("/api/v1/auth/signout")
    fun signout(session: HttpSession): CommonApiResponse<*> {
        signoutService.execute(session)
        return CommonApiResponse.success("로그아웃되었습니다.")
    }
}
