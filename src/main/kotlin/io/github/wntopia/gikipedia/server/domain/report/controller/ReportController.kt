package io.github.wntopia.gikipedia.server.domain.report.controller

import io.github.wntopia.gikipedia.server.domain.report.dto.request.CreateReportReqDto
import io.github.wntopia.gikipedia.server.domain.report.dto.response.ReportPageResDto
import io.github.wntopia.gikipedia.server.domain.report.entity.ReportStatus
import io.github.wntopia.gikipedia.server.domain.report.service.CreateReportService
import io.github.wntopia.gikipedia.server.domain.report.service.QueryReportsService
import io.github.wntopia.gikipedia.server.domain.report.service.ResolveReportService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpSession
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import team.themoment.sdk.response.CommonApiResponse

@Tag(name = "Report", description = "문서 신고 API. 신고 접수는 로그인, 목록 조회·처리는 관리자(ADMIN·ROOT) 권한이 필요하다.")
@Validated
@RestController
@RequestMapping("/api/v1")
class ReportController(
    private val createReportService: CreateReportService,
    private val queryReportsService: QueryReportsService,
    private val resolveReportService: ResolveReportService,
) {
    @Operation(
        summary = "문서 신고",
        description =
            "문서를 신고한다. `revision`을 보내면 그 리비전을 신고하고, 생략하면 문서 전체를 신고한다. " +
                "같은 사용자가 같은 문서에 아직 처리되지 않은 신고를 이미 냈다면 409를 반환한다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "접수 성공"),
            ApiResponse(responseCode = "400", description = "사유가 비어 있거나 1000자를 초과함, 존재하지 않는 리비전"),
            ApiResponse(responseCode = "401", description = "로그인하지 않음"),
            ApiResponse(responseCode = "404", description = "존재하지 않는 문서"),
            ApiResponse(responseCode = "409", description = "같은 문서에 처리되지 않은 신고가 이미 있음"),
        ],
    )
    @PostMapping("/articles/{articleId}/reports")
    fun create(
        @Parameter(description = "문서 ID", example = "1") @PathVariable articleId: Long,
        @Valid @RequestBody reqDto: CreateReportReqDto,
        @Parameter(hidden = true) session: HttpSession,
    ): CommonApiResponse<*> {
        createReportService.execute(articleId, reqDto, session)
        return CommonApiResponse.created("신고가 접수되었습니다.")
    }

    @Operation(
        summary = "신고 목록 조회 (관리자)",
        description = "접수된 신고를 최신 순으로 페이지 단위로 조회한다. `status`로 처리 상태를 걸러낼 수 있고, 생략하면 전체다.",
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
        @Parameter(description = "처리 상태 필터 (OPEN, RESOLVED). 생략하면 전체") @RequestParam(required = false) status:
            ReportStatus?,
        @Parameter(description = "페이지 번호 (0부터, 기본 0)", example = "0")
        @RequestParam(defaultValue = "0")
        @Min(0) page: Int,
        @Parameter(description = "페이지 크기 (1~100, 기본 20)", example = "20")
        @RequestParam(defaultValue = "20")
        @Min(1)
        @Max(100) size: Int,
        @Parameter(hidden = true) session: HttpSession,
    ): ReportPageResDto = queryReportsService.execute(status, page, size, session)

    @Operation(
        summary = "신고 처리 완료 (관리자)",
        description = "신고를 처리 완료(RESOLVED) 상태로 바꾼다. 이미 처리된 신고여도 성공으로 응답한다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "처리 성공"),
            ApiResponse(responseCode = "401", description = "로그인하지 않음"),
            ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
            ApiResponse(responseCode = "404", description = "존재하지 않는 신고"),
        ],
    )
    @PatchMapping("/reports/{reportId}/resolve")
    fun resolve(
        @Parameter(description = "신고 ID", example = "1") @PathVariable reportId: Long,
        @Parameter(hidden = true) session: HttpSession,
    ): CommonApiResponse<*> {
        resolveReportService.execute(reportId, session)
        return CommonApiResponse.success("신고가 처리되었습니다.")
    }
}
