package io.github.wntopia.gikipedia.server.domain.article.controller

import io.github.wntopia.gikipedia.server.domain.article.dto.request.CreateArticleReqDto
import io.github.wntopia.gikipedia.server.domain.article.dto.request.UpdateArticleImageReqDto
import io.github.wntopia.gikipedia.server.domain.article.dto.request.UpdateArticleReqDto
import io.github.wntopia.gikipedia.server.domain.article.dto.response.ArticleResDto
import io.github.wntopia.gikipedia.server.domain.article.service.CreateArticleService
import io.github.wntopia.gikipedia.server.domain.article.service.QueryArticleService
import io.github.wntopia.gikipedia.server.domain.article.service.UpdateArticleImageService
import io.github.wntopia.gikipedia.server.domain.article.service.UpdateArticleService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpSession
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
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
) {
    @Operation(
        summary = "문서 생성",
        description =
            "제목·본문·대표 이미지(선택)로 새 문서를 만든다. `multipart/form-data`로 전송하며 이미지는 최대 10MB. " +
                "생성된 문서는 리비전 1로 히스토리에 기록되고, 제목은 이후 수정할 수 없다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "생성 성공. `data`에 생성된 문서가 담긴다."),
            ApiResponse(responseCode = "400", description = "제목/본문이 비어 있거나 제목이 255자를 초과함"),
            ApiResponse(responseCode = "502", description = "이미지 업로드 실패"),
        ],
    )
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun create(
        @Valid @ModelAttribute reqDto: CreateArticleReqDto,
    ): CommonApiResponse<ArticleResDto> = CommonApiResponse.created("", createArticleService.execute(reqDto))

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
