package io.github.wntopia.gikipedia.server.domain.article.controller

import io.github.wntopia.gikipedia.server.domain.article.dto.request.CreateArticleReqDto
import io.github.wntopia.gikipedia.server.domain.article.dto.request.LookupArticleTitlesReqDto
import io.github.wntopia.gikipedia.server.domain.article.dto.request.UpdateArticleImageReqDto
import io.github.wntopia.gikipedia.server.domain.article.dto.request.UpdateArticleReqDto
import io.github.wntopia.gikipedia.server.domain.article.dto.response.ArticleResDto
import io.github.wntopia.gikipedia.server.domain.article.dto.response.LookupArticleTitlesResDto
import io.github.wntopia.gikipedia.server.domain.article.dto.response.RecentChangeResDto
import io.github.wntopia.gikipedia.server.domain.article.service.CreateArticleService
import io.github.wntopia.gikipedia.server.domain.article.service.LookupArticleTitlesService
import io.github.wntopia.gikipedia.server.domain.article.service.QueryArticleByTitleService
import io.github.wntopia.gikipedia.server.domain.article.service.QueryArticleService
import io.github.wntopia.gikipedia.server.domain.article.service.QueryRecentChangesService
import io.github.wntopia.gikipedia.server.domain.article.service.UpdateArticleImageService
import io.github.wntopia.gikipedia.server.domain.article.service.UpdateArticleService
import io.github.wntopia.gikipedia.server.domain.link.dto.response.BacklinkResDto
import io.github.wntopia.gikipedia.server.domain.link.service.QueryBacklinksService
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
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import team.themoment.sdk.response.CommonApiResponse

@Tag(
    name = "Article",
    description = "문서 생성·조회·수정 API. 모든 응답은 `{ status, code, message, data }` 형태로 감싸지며 실제 값은 `data`에 담긴다.",
)
@RestController
@RequestMapping("/api/v1/articles")
class ArticleController(
    private val createArticleService: CreateArticleService,
    private val queryArticleService: QueryArticleService,
    private val updateArticleService: UpdateArticleService,
    private val updateArticleImageService: UpdateArticleImageService,
    private val queryArticleByTitleService: QueryArticleByTitleService,
    private val lookupArticleTitlesService: LookupArticleTitlesService,
    private val queryBacklinksService: QueryBacklinksService,
    private val queryRecentChangesService: QueryRecentChangesService,
) {
    @Operation(
        summary = "문서 생성",
        description =
            "제목·본문·대표 이미지(선택)로 새 문서를 만든다. `multipart/form-data`로 전송하며 이미지는 최대 10MB. " +
                "생성된 문서는 리비전 1로 히스토리에 기록되고, 제목은 이후 수정할 수 없다. " +
                "로그인한 상태로 만들면 생성자가 기록되어 내 기여 통계의 '새로 만든 문서 수'에 반영된다. " +
                "제목은 앞뒤 공백 제거·유니코드(NFC) 정규화·연속 공백 축소 후 저장되며, 대소문자 구분 없이 유일해야 한다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "생성 성공. `data`에 생성된 문서가 담긴다."),
            ApiResponse(responseCode = "400", description = "제목/본문이 비어 있거나 제목이 255자를 초과함"),
            ApiResponse(responseCode = "409", description = "같은 제목의 문서가 이미 존재함"),
            ApiResponse(responseCode = "502", description = "이미지 업로드 실패"),
        ],
    )
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun create(
        @Valid @ModelAttribute reqDto: CreateArticleReqDto,
        @Parameter(hidden = true) session: HttpSession,
    ): CommonApiResponse<ArticleResDto> = CommonApiResponse.created("", createArticleService.execute(reqDto, session))

    @Operation(
        summary = "제목으로 문서 조회",
        description =
            "위키 링크(`[[문서명]]`)의 제목으로 문서를 조회한다. 제목은 생성 시와 같은 규칙(앞뒤 공백 제거, " +
                "유니코드 정규화, 연속 공백 축소, 대소문자 무시)으로 정규화해 찾는다. 로그인 없이 호출할 수 있다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
            ApiResponse(responseCode = "404", description = "해당 제목의 문서가 없음"),
        ],
    )
    @GetMapping("/by-title")
    fun queryByTitle(
        @Parameter(description = "문서 제목", example = "기숙사 생활 안내") @RequestParam title: String,
    ): ArticleResDto = queryArticleByTitleService.execute(title)

    @Operation(
        summary = "최근 변경 문서 목록 조회",
        description =
            "최근에 수정된 문서를 최신 순으로 문서당 한 항목씩 반환한다. 같은 문서를 여러 번 수정해도 한 번만 나오고, " +
                "오늘(한국 시간 0시부터) 편집 횟수와 편집한 사용자 수가 함께 내려온다. " +
                "\"○○ 외 N명이 오늘 M회 수정\"은 `lastEditorName`, `todayEditorCount - 1`, `todayEditCount`로 만들 수 있다. " +
                "생성 이후 수정되지 않은 문서는 `newlyCreated`가 true다. 로그인 없이 호출할 수 있다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
            ApiResponse(responseCode = "400", description = "page 또는 size가 범위를 벗어남"),
        ],
    )
    @GetMapping("/recent-changes")
    fun recentChanges(
        @Parameter(description = "페이지 번호 (0부터, 기본 0)", example = "0")
        @RequestParam(defaultValue = "0")
        @Min(0) page: Int,
        @Parameter(description = "페이지 크기 (1~100, 기본 20)", example = "20")
        @RequestParam(defaultValue = "20")
        @Min(1)
        @Max(100) size: Int,
    ): PageResDto<RecentChangeResDto> = queryRecentChangesService.execute(page, size)

    @Operation(
        summary = "역링크 조회",
        description =
            "지정한 제목을 `[[문서명]]`으로 링크하고 있는 문서 목록을 제목 순으로 조회한다. 아직 만들어지지 않은 " +
                "문서의 제목으로도 조회할 수 있다. 링크 판정은 문서 검색의 표시용 텍스트 변환과 같은 규칙을 따른다 — " +
                "한 줄 안의 `[[문서명]]`만 링크이고, 중첩되거나 `|`(별칭)가 들어간 구문은 링크로 보지 않으며 `#`은 제목의 " +
                "일부다. 자기 자신을 링크한 경우는 제외한다. 로그인 없이 호출할 수 있다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공. 링크한 문서가 없으면 빈 목록이다."),
            ApiResponse(responseCode = "400", description = "page 또는 size가 범위를 벗어남"),
        ],
    )
    @GetMapping("/backlinks")
    fun backlinks(
        @Parameter(description = "링크 대상 문서 제목", example = "기숙사 생활 안내") @RequestParam title: String,
        @Parameter(description = "페이지 번호 (0부터, 기본 0)", example = "0")
        @RequestParam(defaultValue = "0")
        @Min(0) page: Int,
        @Parameter(description = "페이지 크기 (1~100, 기본 20)", example = "20")
        @RequestParam(defaultValue = "20")
        @Min(1)
        @Max(100) size: Int,
    ): PageResDto<BacklinkResDto> = queryBacklinksService.execute(title, page, size)

    @Operation(
        summary = "제목 존재 여부 일괄 조회",
        description =
            "본문에 나온 여러 위키 링크 제목이 실제 문서인지 한 번에 확인한다(최대 200개). 요청한 순서대로 " +
                "제목별 존재 여부와 문서 ID를 반환하며, 존재하지 않는 제목의 `id`는 null이다. 로그인 없이 호출할 수 있다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공. 존재하지 않는 제목도 `exists=false`로 포함된다."),
            ApiResponse(responseCode = "400", description = "제목 목록이 비어 있거나 200개를 초과함"),
        ],
    )
    @PostMapping("/titles/lookup")
    fun lookupTitles(
        @Valid @RequestBody reqDto: LookupArticleTitlesReqDto,
    ): LookupArticleTitlesResDto = lookupArticleTitlesService.execute(reqDto)

    @Operation(
        summary = "문서 조회",
        description = "문서의 최신 내용을 조회한다. 로그인 없이 호출할 수 있다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
            ApiResponse(responseCode = "404", description = "존재하지 않는 문서"),
        ],
    )
    @GetMapping("/{articleId}")
    fun query(
        @Parameter(description = "문서 ID", example = "1") @PathVariable articleId: Long,
    ): ArticleResDto = queryArticleService.execute(articleId)

    @Operation(
        summary = "문서 수정",
        description =
            "문서 본문을 교체하고, 이미지를 함께 보내면 대표 이미지도 교체한다(안 보내면 기존 이미지 유지). " +
                "로그인한 학생 계정만 가능하며 수정할 때마다 새 리비전이 생성된다. 제목은 수정할 수 없다. " +
                "편집 요약(`summary`, 최대 500자)과 가벼운 수정 여부(`minorEdit`)를 선택적으로 함께 보낼 수 있다. " +
                "`multipart/form-data`로 전송하며 이미지는 최대 10MB.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "수정 성공. `data`에 수정된 문서가 담긴다."),
            ApiResponse(responseCode = "400", description = "본문이 비어 있음"),
            ApiResponse(responseCode = "401", description = "로그인하지 않았거나 학생 계정이 아님"),
            ApiResponse(responseCode = "404", description = "존재하지 않는 문서 (401보다 먼저 판정됨)"),
            ApiResponse(responseCode = "502", description = "이미지 업로드 실패"),
        ],
    )
    @PutMapping("/{articleId}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun update(
        @Parameter(description = "문서 ID", example = "1") @PathVariable articleId: Long,
        @Valid @ModelAttribute reqDto: UpdateArticleReqDto,
        @Parameter(hidden = true) session: HttpSession,
    ): ArticleResDto = updateArticleService.execute(articleId, reqDto, session)

    /** 실시간 공동편집 중에도 대표 이미지만 별도로 교체할 수 있는 전용 엔드포인트. content는 건드리지 않는다. */
    @Operation(
        summary = "문서 대표 이미지 교체",
        description =
            "본문은 건드리지 않고 대표 이미지만 교체한다. 실시간 공동편집 중에도 사용할 수 있으며, " +
                "공동편집 중인 다른 사용자에게도 바뀐 이미지가 실시간으로 전달된다. 새 리비전은 만들지 않는다. " +
                "로그인한 학생 계정만 가능하며 이미지는 최대 10MB.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "교체 성공. `data`에 반영된 문서가 담긴다."),
            ApiResponse(responseCode = "400", description = "이미지가 없거나 빈 파일"),
            ApiResponse(responseCode = "401", description = "로그인하지 않았거나 학생 계정이 아님"),
            ApiResponse(responseCode = "404", description = "존재하지 않는 문서"),
            ApiResponse(responseCode = "502", description = "이미지 업로드 실패"),
        ],
    )
    @PatchMapping("/{articleId}/image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun updateImage(
        @Parameter(description = "문서 ID", example = "1") @PathVariable articleId: Long,
        @Valid @ModelAttribute reqDto: UpdateArticleImageReqDto,
        @Parameter(hidden = true) session: HttpSession,
    ): ArticleResDto = updateArticleImageService.execute(articleId, reqDto, session)
}
