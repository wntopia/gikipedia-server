package io.github.wntopia.gikipedia.server.domain.article.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import org.hibernate.validator.constraints.CodePointLength

data class SearchArticleReqDto(
    @field:Schema(description = "검색어. 최대 100 코드포인트, 양끝 공백 제거, 내부 공백 유지", example = "급식 안내")
    @field:NotBlank(message = "검색어는 비어 있을 수 없습니다.")
    @field:CodePointLength(max = 100, message = "검색어는 100자를 초과할 수 없습니다.")
    val q: String = "",
    @field:Schema(description = "0부터 시작하는 페이지 번호", defaultValue = "0", minimum = "0")
    @field:Min(0, message = "페이지 번호는 0 이상이어야 합니다.")
    val page: Int = 0,
    @field:Schema(description = "페이지 크기", defaultValue = "20", minimum = "1", maximum = "50")
    @field:Min(1, message = "페이지 크기는 1 이상이어야 합니다.")
    @field:Max(50, message = "페이지 크기는 50 이하여야 합니다.")
    val size: Int = 20,
)
