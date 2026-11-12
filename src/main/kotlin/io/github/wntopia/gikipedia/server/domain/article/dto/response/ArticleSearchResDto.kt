package io.github.wntopia.gikipedia.server.domain.article.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class ArticleSearchResDto(
    val items: List<ArticleSearchItemResDto>,
    @field:Schema(description = "요청한 페이지 번호 (0부터 시작)")
    val page: Int,
    @field:Schema(description = "요청한 페이지 크기")
    val size: Int,
    @field:Schema(description = "전체 일치 문서 수. 동시 수정 시 목록과 차이가 날 수 있음")
    val totalElements: Long,
    @field:Schema(description = "전체 페이지 수. 일치 문서가 없으면 0")
    val totalPages: Long,
)
