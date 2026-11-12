package io.github.wntopia.gikipedia.server.domain.stats.controller

import io.github.wntopia.gikipedia.server.domain.stats.dto.response.WikiStatsResDto
import io.github.wntopia.gikipedia.server.domain.stats.service.QueryWikiStatsService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Stats", description = "위키 전체 현황 API")
@RestController
@RequestMapping("/api/v1/stats")
class WikiStatsController(
    private val queryWikiStatsService: QueryWikiStatsService,
) {
    @Operation(
        summary = "위키 현황 조회",
        description =
            "전체 문서 수, 이번 주 편집 수, 가입 사용자 수를 한 번에 반환한다. 집계 비용을 줄이려고 결과를 짧은 시간(기본 60초) " +
                "재사용하므로 문서를 만들거나 수정한 뒤 값이 반영되기까지 그만큼 걸릴 수 있다. 로그인 없이 호출할 수 있다.",
    )
    @ApiResponses(value = [ApiResponse(responseCode = "200", description = "조회 성공")])
    @GetMapping
    fun stats(): WikiStatsResDto = queryWikiStatsService.execute()
}
