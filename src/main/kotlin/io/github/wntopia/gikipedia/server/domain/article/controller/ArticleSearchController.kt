package io.github.wntopia.gikipedia.server.domain.article.controller

import io.github.wntopia.gikipedia.server.domain.article.dto.request.SearchArticleReqDto
import io.github.wntopia.gikipedia.server.domain.article.dto.response.ArticleSearchResDto
import io.github.wntopia.gikipedia.server.domain.article.service.SearchArticleService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.HttpStatus
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import team.themoment.sdk.response.CommonApiResponse

@Tag(name = "Article")
@RestController
@RequestMapping("/api/v1/articles")
class ArticleSearchController(
    private val searchArticleService: SearchArticleService,
) {
    @Operation(
        summary = "문서 검색",
        description =
            "제목과 위키 문법을 제거한 본문에서 검색어 전체를 연속 문자열로 찾는다. " +
                "로그인 없이 호출하며 영문 A–Z 대소문자를 무시한다. 양끝 공백만 제거하고 내부 공백·특수문자는 그대로 비교한다. " +
                "제목 정확히 일치 → 제목 포함 → 본문 포함 순이며, 각 그룹은 원본 수정 시각·문서 ID 내림차순이다. " +
                "snippet은 표시용 문자열과 highlight 배열이며 HTML로 해석하지 않는다. 본문이 비면 빈 배열이다. " +
                "MongoDB 비동기 읽기 뷰를 사용하므로 저장 직후 반영을 보장하지 않는다. " +
                "동시 수정 시 전체 건수와 목록이 다를 수 있고 페이지 사이 중복·누락이 가능하다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "검색 성공. 결과가 없으면 items=[], totalElements=0, totalPages=0"),
            ApiResponse(responseCode = "400", description = "빈 검색어, 100자 초과, NUL 문자, 음수 페이지, 페이지 크기 1~50 위반"),
            ApiResponse(responseCode = "503", description = "MongoDB 연결 실패 또는 검색 실행 시간 제한 초과"),
        ],
    )
    @GetMapping
    fun search(
        @Valid @ModelAttribute @ParameterObject reqDto: SearchArticleReqDto,
    ): ArticleSearchResDto = searchArticleService.execute(reqDto)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun invalidInput() = CommonApiResponse.error("검색어와 페이지 입력 값을 확인해 주세요.", HttpStatus.BAD_REQUEST)
}
