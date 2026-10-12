package io.github.wntopia.gikipedia.server.domain.report.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class ReportPageResDto(
    @field:Schema(description = "현재 페이지의 신고 목록")
    val items: List<ReportResDto>,
    @field:Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
    val page: Int,
    @field:Schema(description = "페이지 크기", example = "20")
    val size: Int,
    @field:Schema(description = "전체 신고 수", example = "5")
    val totalElements: Long,
    @field:Schema(description = "다음 페이지가 있는지 여부")
    val hasNext: Boolean,
)
