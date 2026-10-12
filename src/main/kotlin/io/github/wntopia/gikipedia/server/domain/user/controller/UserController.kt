package io.github.wntopia.gikipedia.server.domain.user.controller

import io.github.wntopia.gikipedia.server.domain.user.dto.response.ContributionResDto
import io.github.wntopia.gikipedia.server.domain.user.dto.response.ContributionStatsResDto
import io.github.wntopia.gikipedia.server.domain.user.service.QueryMyContributionStatsService
import io.github.wntopia.gikipedia.server.domain.user.service.QueryMyContributionsService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpSession
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "User", description = "로그인한 사용자 본인의 기여 통계·목록 API. 모두 로그인(세션 쿠키)이 필요하다.")
@Validated
@RestController
@RequestMapping("/api/v1/users/me")
class UserController(
    private val queryMyContributionStatsService: QueryMyContributionStatsService,
    private val queryMyContributionsService: QueryMyContributionsService,
) {
    @Operation(
        summary = "내 기여 통계 조회",
        description =
            "가입일(첫 로그인 시각), 총 편집 수, 새로 만든 문서 수, 오늘 편집 수를 반환한다. " +
                "총 편집 수는 기존 문서를 수정한 횟수이며 문서 생성은 포함하지 않는다. 오늘은 한국 시간 기준이다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
            ApiResponse(responseCode = "401", description = "로그인하지 않음"),
        ],
    )
    @GetMapping("/contributions/stats")
    fun stats(
        @Parameter(hidden = true) session: HttpSession,
    ): ContributionStatsResDto = queryMyContributionStatsService.execute(session)

    @Operation(
        summary = "내 최근 기여 목록 조회",
        description = "내가 만들거나 수정한 문서를 최신 순으로 반환한다. 같은 문서를 여러 번 수정했으면 수정마다 한 항목이다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
            ApiResponse(responseCode = "400", description = "limit이 범위(1~100)를 벗어남"),
            ApiResponse(responseCode = "401", description = "로그인하지 않음"),
        ],
    )
    @GetMapping("/contributions")
    fun contributions(
        @Parameter(description = "가져올 개수 (1~100, 기본 20)", example = "20")
        @RequestParam(defaultValue = "20")
        @Min(1)
        @Max(100) limit: Int,
        @Parameter(hidden = true) session: HttpSession,
    ): List<ContributionResDto> = queryMyContributionsService.execute(session, limit)
}
