package io.github.wntopia.gikipedia.server.global.dto

import io.swagger.v3.oas.annotations.media.Schema

/** 목록 API의 공통 페이지 응답. [page]는 0부터 시작한다. */
data class PageResDto<T>(
    @field:Schema(description = "현재 페이지 항목")
    val items: List<T>,
    @field:Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
    val page: Int,
    @field:Schema(description = "페이지 크기", example = "20")
    val size: Int,
    @field:Schema(description = "전체 항목 수", example = "57")
    val totalElements: Long,
    @field:Schema(description = "다음 페이지가 있는지 여부")
    val hasNext: Boolean,
)
