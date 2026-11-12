package io.github.wntopia.gikipedia.server.domain.report.controller

import io.github.wntopia.gikipedia.server.domain.report.dto.request.CreateReportReqDto
import io.github.wntopia.gikipedia.server.domain.report.dto.response.ReportResDto
import io.github.wntopia.gikipedia.server.domain.report.service.CreateReportService
import io.github.wntopia.gikipedia.server.domain.report.service.QueryReportsService
import io.github.wntopia.gikipedia.server.global.dto.PageResDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpSession
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import team.themoment.sdk.response.CommonApiResponse

@Tag(name = "Report", description = "문서 리비전 신고 API. 신고 접수는 로그인, 신고 목록은 관리자 권한이 필요하다.")
@RestController
@RequestMapping("/api/v1")
class ReportController(
    private val createReportService: CreateReportService,
    private val queryReportsService: QueryReportsService,
) {
    @Operation(
        summary = "리비전 신고",
        description =
            "문서의 특정 리비전(편집)을 사유와 함께 신고한다. 로그인한 사용자만 가능하며, 같은 리비전은 한 사람당 한 번만 " +
                "신고할 수 있다. 최신 내용을 신고하려면 문서의 최신 리비전 번호를 보낸다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "신고 접수"),
            ApiResponse(responseCode = "400", description = "사유가 비어 있거나 500자를 초과함"),
            ApiResponse(responseCode = "401", description = "로그인하지 않음"),
            ApiResponse(responseCode = "404", description = "존재하지 않는 문서 또는 리비전"),
            ApiResponse(responseCode = "409", description = "이미 신고한 리비전"),
        ],
    )
    @PostMapping("/articles/{articleId}/revisions/{revision}/reports")
    fun create(
        @Parameter(description = "문서 ID", example = "1") @PathVariable articleId: Long,
        @Parameter(description = "리비전 번호 (1부터 시작)", example = "3") @PathVariable revision: Int,
        @Valid @RequestBody reqDto: CreateReportReqDto,
        @Parameter(hidden = true) session: HttpSession,
    ): CommonApiResponse<*> {
        createReportService.execute(articleId, revision, reqDto, session)
        return CommonApiResponse.created("신고가 접수되었습니다.")
    }

    @Operation(
        summary = "신고 목록 조회 (관리자)",
        description = "접수된 신고를 최근 접수 순으로 페이지 단위로 반환한다. ADMIN·ROOT 권한만 호출할 수 있다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
            ApiResponse(responseCode = "400", description = "page 또는 size가 범위를 벗어남"),
            ApiResponse(responseCode = "401", description = "로그인하지 않음"),
            ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
        ],
    )
    @GetMapping("/reports")
    fun list(
        @Parameter(description = "페이지 번호 (0부터, 기본 0)", example = "0")
        @RequestParam(defaultValue = "0")
        @Min(0) page: Int,
        @Parameter(description = "페이지 크기 (1~100, 기본 20)", example = "20")
        @RequestParam(defaultValue = "20")
        @Min(1)
        @Max(100) size: Int,
        @Parameter(hidden = true) session: HttpSession,
    ): PageResDto<ReportResDto> = queryReportsService.execute(session, page, size)
}
