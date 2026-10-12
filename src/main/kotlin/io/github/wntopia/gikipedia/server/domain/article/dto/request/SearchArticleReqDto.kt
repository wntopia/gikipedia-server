package io.github.wntopia.gikipedia.server.domain.article.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import org.hibernate.validator.constraints.CodePointLength

data class SearchArticleReqDto(
    @field:Schema(description = "검색어. 최대 $MAX_QUERY_LENGTH 코드포인트, 양끝 공백 제거, 내부 공백 유지", example = "급식 안내")
    @field:NotBlank(message = "검색어는 비어 있을 수 없습니다.")
    @field:CodePointLength(max = MAX_QUERY_LENGTH, message = "검색어는 ${MAX_QUERY_LENGTH}자를 초과할 수 없습니다.")
    val q: String = "",
    @field:Schema(description = "0부터 시작하는 페이지 번호", defaultValue = "0", minimum = "0")
    @field:Min(0, message = "페이지 번호는 0 이상이어야 합니다.")
    val page: Int = 0,
    @field:Schema(description = "페이지 크기", defaultValue = "20", minimum = "1", maximum = "$MAX_PAGE_SIZE")
    @field:Min(1, message = "페이지 크기는 1 이상이어야 합니다.")
    @field:Max(MAX_PAGE_SIZE.toLong(), message = "페이지 크기는 $MAX_PAGE_SIZE 이하여야 합니다.")
    val size: Int = 20,
) {
    companion object {
        const val MAX_QUERY_LENGTH = 100
        const val MAX_PAGE_SIZE = 50
    }
}
