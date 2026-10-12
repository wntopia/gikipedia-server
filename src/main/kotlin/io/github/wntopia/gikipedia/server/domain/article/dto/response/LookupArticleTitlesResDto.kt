package io.github.wntopia.gikipedia.server.domain.article.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class LookupArticleTitlesResDto(
    @field:Schema(description = "요청한 제목 순서 그대로의 조회 결과")
    val items: List<Item>,
) {
    data class Item(
        @field:Schema(description = "요청에 담겼던 제목 (원문 그대로)")
        val title: String,
        @field:Schema(description = "해당 제목의 문서가 존재하는지 여부")
        val exists: Boolean,
        @field:Schema(description = "문서 ID (존재하지 않으면 null)")
        val id: Long?,
    )
}
