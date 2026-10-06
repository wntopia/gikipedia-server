package io.github.wntopia.gikipedia.server.domain.auth.controller

import io.github.wntopia.gikipedia.server.domain.auth.dto.request.DataGsmOAuthCallbackReqDto
import io.github.wntopia.gikipedia.server.domain.auth.dto.response.AuthStatusResDto
import io.github.wntopia.gikipedia.server.domain.auth.service.QueryAuthStatusService
import io.github.wntopia.gikipedia.server.domain.auth.service.QueryAuthorizationUriService
import io.github.wntopia.gikipedia.server.domain.auth.service.SigninService
import io.github.wntopia.gikipedia.server.domain.auth.service.SignoutService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpSession
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import team.themoment.sdk.response.CommonApiResponse

@Tag(
    name = "Auth",
    description =
        "DataGSM OAuth 기반 세션 로그인 API. 인증은 세션 쿠키로 유지되므로 프론트는 요청 시 " +
            "`credentials: 'include'`(axios는 `withCredentials: true`)로 쿠키를 함께 보내야 한다.",
)
@RestController
class AuthController(
    private val queryAuthorizationUriService: QueryAuthorizationUriService,
    private val signinService: SigninService,
    private val queryAuthStatusService: QueryAuthStatusService,
    private val signoutService: SignoutService,
) {
    @Operation(
        summary = "DataGSM 로그인 시작",
        description =
            "DataGSM 로그인 페이지로 302 리다이렉트한다. fetch/axios가 아니라 " +
                "`window.location.href`로 이 URL에 브라우저를 이동시켜야 한다. " +
                "로그인이 끝나면 DataGSM이 콜백 API를 거쳐 프론트의 성공/실패 페이지로 다시 보낸다.",
    )
    @ApiResponses(
        value = [ApiResponse(responseCode = "302", description = "DataGSM 인가 페이지로 리다이렉트")],
    )
    @GetMapping("/api/v1/oauth/datagsm/signin")
    fun signin(
        @Parameter(hidden = true) session: HttpSession,
    ): ResponseEntity<Void> = queryAuthorizationUriService.execute(session)

    @Operation(
        summary = "DataGSM 로그인 콜백",
        description =
            "DataGSM이 로그인 후 호출하는 콜백으로, 프론트에서 직접 호출하지 않는다. 처리 결과에 따라 프론트로 302 리다이렉트한다.\n\n" +
                "- 성공: 성공 페이지 `?authenticated=true&student={학생 계정 여부}`\n" +
                "- 실패: 실패 페이지 `?reason={실패 사유}`",
    )
    @ApiResponses(
        value = [ApiResponse(responseCode = "302", description = "프론트 성공/실패 페이지로 리다이렉트")],
    )
    @GetMapping("/api/v1/oauth/datagsm/callback")
    fun callback(
        @ModelAttribute reqDto: DataGsmOAuthCallbackReqDto,
        @Parameter(hidden = true) session: HttpSession,
    ): ResponseEntity<Void> = signinService.execute(reqDto, session)

    @Operation(
        summary = "내 로그인 정보 조회",
        description = "현재 세션의 로그인 사용자 정보를 반환한다. 앱 진입 시 로그인 여부를 판단하는 용도로 쓴다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "로그인 상태. `data.user`에 DataGSM 사용자 정보가 담긴다."),
            ApiResponse(responseCode = "401", description = "로그인하지 않음"),
        ],
    )
    @GetMapping("/api/v1/auth/me")
    fun me(
        @Parameter(hidden = true) session: HttpSession,
    ): AuthStatusResDto = queryAuthStatusService.execute(session)

    @Operation(
        summary = "로그아웃",
        description = "현재 세션을 무효화한다. 로그인하지 않은 상태에서 호출해도 성공한다.",
    )
    @ApiResponses(
        value = [ApiResponse(responseCode = "200", description = "로그아웃 성공")],
    )
    @PostMapping("/api/v1/auth/signout")
    fun signout(
        @Parameter(hidden = true) session: HttpSession,
    ): CommonApiResponse<*> {
        signoutService.execute(session)
        return CommonApiResponse.success("로그아웃되었습니다.")
    }
}
