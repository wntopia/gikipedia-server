package io.github.wntopia.gikipedia.server.domain.history.controller

import io.github.wntopia.gikipedia.server.domain.history.dto.response.ArticleRevisionResDto
import io.github.wntopia.gikipedia.server.domain.history.dto.response.ArticleRevisionSummaryResDto
import io.github.wntopia.gikipedia.server.domain.history.dto.response.CacheStatsResDto
import io.github.wntopia.gikipedia.server.domain.history.service.ReconstructArticleService
import io.github.wntopia.gikipedia.server.domain.history.service.ReconstructCacheStatsService
import io.github.wntopia.gikipedia.server.global.security.session.AuthenticationReader
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpSession
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Article History", description = "문서 버전(리비전) 히스토리 조회 API. 리비전 번호는 1(생성 시점)부터 수정마다 1씩 증가한다.")
@RestController
@RequestMapping("/api/v1/articles")
class ArticleHistoryController(
    private val reconstructArticleService: ReconstructArticleService,
    private val reconstructCacheStatsService: ReconstructCacheStatsService,
    private val authenticationReader: AuthenticationReader,
) {
    /** 특정 리비전 시점으로 재구성된 문서. */
    @Operation(
        summary = "특정 리비전의 문서 조회",
        description =
            "지정한 리비전 시점의 문서 본문을 조회한다. 버전 히스토리에서 과거 버전을 보여줄 때 사용한다. " +
                "제목은 수정할 수 없으므로 항상 현재 제목이 내려간다. `latest`가 true면 현재 최신 버전이다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
            ApiResponse(responseCode = "404", description = "존재하지 않는 문서이거나 범위를 벗어난 리비전"),
        ],
    )
    @GetMapping("/{articleId}/revisions/{revision}")
    fun getRevision(
        @Parameter(description = "문서 ID", example = "1") @PathVariable articleId: Long,
        @Parameter(description = "리비전 번호 (1부터 시작)", example = "1") @PathVariable revision: Int,
    ): ArticleRevisionResDto = reconstructArticleService.reconstruct(articleId, revision)

    /** 문서의 리비전 목록(최신 순). */
    @Operation(
        summary = "문서 리비전 목록 조회",
        description = "문서의 모든 리비전을 최신 순으로 조회한다. 본문 없이 리비전 번호·편집자·편집 시각만 담긴다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
            ApiResponse(responseCode = "404", description = "존재하지 않는 문서"),
        ],
    )
    @GetMapping("/{articleId}/revisions")
    fun listRevisions(
        @Parameter(description = "문서 ID", example = "1") @PathVariable articleId: Long,
    ): List<ArticleRevisionSummaryResDto> = reconstructArticleService.listRevisions(articleId)

    /** 재구성 캐시 히트/미스 통계. */
    @Operation(
        summary = "리비전 캐시 통계 조회 (관리자)",
        description = "과거 리비전 재구성 캐시의 히트/미스 통계를 조회한다. 운영 모니터링용이며 ADMIN·ROOT 권한만 호출할 수 있다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
            ApiResponse(responseCode = "401", description = "로그인하지 않음"),
            ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
        ],
    )
    @GetMapping("/revisions/cache-stats")
    fun cacheStats(
        @Parameter(hidden = true) session: HttpSession,
    ): CacheStatsResDto {
        authenticationReader.requireAdmin(session)
        return reconstructCacheStatsService.stats()
    }
}
