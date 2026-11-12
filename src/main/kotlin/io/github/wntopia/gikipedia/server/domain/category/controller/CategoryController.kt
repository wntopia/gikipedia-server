package io.github.wntopia.gikipedia.server.domain.category.controller

import io.github.wntopia.gikipedia.server.domain.category.dto.request.CreateCategoryReqDto
import io.github.wntopia.gikipedia.server.domain.category.dto.response.CategoryArticleResDto
import io.github.wntopia.gikipedia.server.domain.category.dto.response.CategoryListItemResDto
import io.github.wntopia.gikipedia.server.domain.category.dto.response.CategoryResDto
import io.github.wntopia.gikipedia.server.domain.category.service.CreateCategoryService
import io.github.wntopia.gikipedia.server.domain.category.service.QueryCategoriesService
import io.github.wntopia.gikipedia.server.domain.category.service.QueryCategoryArticlesService
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

@Tag(name = "Category", description = "문서 분류 API. 문서는 생성·수정 시 `categoryId`로 분류를 지정한다.")
@RestController
@RequestMapping("/api/v1/categories")
class CategoryController(
    private val createCategoryService: CreateCategoryService,
    private val queryCategoriesService: QueryCategoriesService,
    private val queryCategoryArticlesService: QueryCategoryArticlesService,
) {
    @Operation(
        summary = "분류 생성 (관리자)",
        description = "새 분류를 만든다. ADMIN·ROOT 권한만 호출할 수 있고, 이름은 앞뒤 공백을 제거한 뒤 중복될 수 없다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "생성 성공. `data`에 만들어진 분류가 담긴다."),
            ApiResponse(responseCode = "400", description = "이름이 비어 있거나 50자를 초과함"),
            ApiResponse(responseCode = "401", description = "로그인하지 않음"),
            ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
            ApiResponse(responseCode = "409", description = "같은 이름의 분류가 이미 존재함"),
        ],
    )
    @PostMapping
    fun create(
        @Valid @RequestBody reqDto: CreateCategoryReqDto,
        @Parameter(hidden = true) session: HttpSession,
    ): CommonApiResponse<CategoryResDto> = CommonApiResponse.created("", createCategoryService.execute(reqDto, session))

    @Operation(
        summary = "분류 목록 조회",
        description = "모든 분류와 분류별 문서 수를 이름 순으로 반환한다. 홈의 '분류' 목록에 쓴다. 로그인 없이 호출할 수 있다.",
    )
    @ApiResponses(value = [ApiResponse(responseCode = "200", description = "조회 성공")])
    @GetMapping
    fun list(): List<CategoryListItemResDto> = queryCategoriesService.execute()

    @Operation(
        summary = "분류별 문서 목록 조회",
        description = "해당 분류에 속한 문서를 최근 수정 순으로 페이지 단위로 반환한다. 로그인 없이 호출할 수 있다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
            ApiResponse(responseCode = "400", description = "page 또는 size가 범위를 벗어남"),
            ApiResponse(responseCode = "404", description = "존재하지 않는 분류"),
        ],
    )
    @GetMapping("/{categoryId}/articles")
    fun articles(
        @Parameter(description = "분류 ID", example = "1") @PathVariable categoryId: Long,
        @Parameter(description = "페이지 번호 (0부터, 기본 0)", example = "0")
        @RequestParam(defaultValue = "0")
        @Min(0) page: Int,
        @Parameter(description = "페이지 크기 (1~100, 기본 20)", example = "20")
        @RequestParam(defaultValue = "20")
        @Min(1)
        @Max(100) size: Int,
    ): PageResDto<CategoryArticleResDto> = queryCategoryArticlesService.execute(categoryId, page, size)
}
