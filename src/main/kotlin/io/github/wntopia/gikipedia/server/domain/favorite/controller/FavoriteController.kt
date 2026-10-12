package io.github.wntopia.gikipedia.server.domain.favorite.controller

import io.github.wntopia.gikipedia.server.domain.favorite.dto.response.FavoriteResDto
import io.github.wntopia.gikipedia.server.domain.favorite.dto.response.FavoriteStatusResDto
import io.github.wntopia.gikipedia.server.domain.favorite.service.AddFavoriteService
import io.github.wntopia.gikipedia.server.domain.favorite.service.QueryFavoriteStatusService
import io.github.wntopia.gikipedia.server.domain.favorite.service.QueryMyFavoritesService
import io.github.wntopia.gikipedia.server.domain.favorite.service.RemoveFavoriteService
import io.github.wntopia.gikipedia.server.global.dto.PageResDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpSession
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import team.themoment.sdk.response.CommonApiResponse

@Tag(name = "Favorite", description = "문서 즐겨찾기 API. 추가·삭제·내 목록은 로그인(세션 쿠키)이 필요하다.")
@Validated
@RestController
@RequestMapping("/api/v1")
class FavoriteController(
    private val addFavoriteService: AddFavoriteService,
    private val removeFavoriteService: RemoveFavoriteService,
    private val queryFavoriteStatusService: QueryFavoriteStatusService,
    private val queryMyFavoritesService: QueryMyFavoritesService,
) {
    @Operation(
        summary = "즐겨찾기에 추가",
        description = "문서를 내 즐겨찾기에 추가한다. 이미 추가된 문서를 다시 추가해도 한 번만 들어가며 성공으로 응답한다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "추가 성공 (이미 있었어도 성공)"),
            ApiResponse(responseCode = "401", description = "로그인하지 않음"),
            ApiResponse(responseCode = "404", description = "존재하지 않는 문서"),
        ],
    )
    @PutMapping("/articles/{articleId}/favorite")
    fun add(
        @Parameter(description = "문서 ID", example = "1") @PathVariable articleId: Long,
        @Parameter(hidden = true) session: HttpSession,
    ): CommonApiResponse<*> {
        addFavoriteService.execute(articleId, session)
        return CommonApiResponse.success("즐겨찾기에 추가되었습니다.")
    }

    @Operation(
        summary = "즐겨찾기에서 삭제",
        description = "문서를 내 즐겨찾기에서 뺀다. 즐겨찾기에 없던 문서여도 성공으로 응답한다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "삭제 성공 (원래 없었어도 성공)"),
            ApiResponse(responseCode = "401", description = "로그인하지 않음"),
        ],
    )
    @DeleteMapping("/articles/{articleId}/favorite")
    fun remove(
        @Parameter(description = "문서 ID", example = "1") @PathVariable articleId: Long,
        @Parameter(hidden = true) session: HttpSession,
    ): CommonApiResponse<*> {
        removeFavoriteService.execute(articleId, session)
        return CommonApiResponse.success("즐겨찾기에서 삭제되었습니다.")
    }

    @Operation(
        summary = "즐겨찾기 여부 조회",
        description = "문서가 내 즐겨찾기에 들어 있는지 확인한다. 문서 상세 화면은 비로그인도 열 수 있으므로 로그인하지 않았으면 401이 아니라 `favorited=false`를 반환한다.",
    )
    @ApiResponses(value = [ApiResponse(responseCode = "200", description = "조회 성공")])
    @GetMapping("/articles/{articleId}/favorite")
    fun status(
        @Parameter(description = "문서 ID", example = "1") @PathVariable articleId: Long,
        @Parameter(hidden = true) session: HttpSession,
    ): FavoriteStatusResDto = queryFavoriteStatusService.execute(articleId, session)

    @Operation(
        summary = "내 즐겨찾기 목록 조회",
        description = "내가 즐겨찾기한 문서를 최근에 추가한 순으로 페이지 단위로 반환한다. 문서 ID와 제목이 담긴다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
            ApiResponse(responseCode = "400", description = "page 또는 size가 범위를 벗어남"),
            ApiResponse(responseCode = "401", description = "로그인하지 않음"),
        ],
    )
    @GetMapping("/users/me/favorites")
    fun myFavorites(
        @Parameter(description = "페이지 번호 (0부터, 기본 0)", example = "0")
        @RequestParam(defaultValue = "0")
        @Min(0) page: Int,
        @Parameter(description = "페이지 크기 (1~100, 기본 20)", example = "20")
        @RequestParam(defaultValue = "20")
        @Min(1)
        @Max(100) size: Int,
        @Parameter(hidden = true) session: HttpSession,
    ): PageResDto<FavoriteResDto> = queryMyFavoritesService.execute(session, page, size)
}
