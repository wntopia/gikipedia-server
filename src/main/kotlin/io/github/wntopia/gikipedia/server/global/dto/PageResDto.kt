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
) {
    companion object {
        /** 이미 전체를 메모리에 가진 목록에서 [page]번째 페이지를 잘라낸다. */
        fun <T> sliceOf(
            all: List<T>,
            page: Int,
            size: Int,
        ): PageResDto<T> {
            val from = minOf(page.toLong() * size, all.size.toLong()).toInt()
            val to = minOf(from.toLong() + size, all.size.toLong()).toInt()
            return PageResDto(all.subList(from, to), page, size, all.size.toLong(), to < all.size)
        }
    }
}
