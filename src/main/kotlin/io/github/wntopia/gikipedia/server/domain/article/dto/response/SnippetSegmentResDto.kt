package io.github.wntopia.gikipedia.server.domain.article.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class SnippetSegmentResDto(
    @field:Schema(description = "HTML로 해석하지 않는 표시용 텍스트")
    val text: String,
    @field:Schema(description = "검색어와 일치하여 강조할 구간인지 여부")
    val highlight: Boolean,
)
