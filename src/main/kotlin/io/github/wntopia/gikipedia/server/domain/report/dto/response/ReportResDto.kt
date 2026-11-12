package io.github.wntopia.gikipedia.server.domain.report.dto.response

import io.github.wntopia.gikipedia.server.domain.report.repository.ReportView
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

data class ReportResDto(
    @field:Schema(description = "신고 ID", example = "1")
    val id: Long,
    @field:Schema(description = "문서 ID", example = "1")
    val articleId: Long,
    @field:Schema(description = "문서 제목")
    val title: String,
    @field:Schema(description = "신고된 리비전 번호", example = "3")
    val revision: Int,
    @field:Schema(description = "신고 사유")
    val reason: String,
    @field:Schema(description = "신고한 사용자의 DataGSM 계정 ID", example = "10")
    val reporterUserId: Long,
    @field:Schema(description = "신고 접수 시각 (ISO-8601, UTC)")
    val reportedAt: Instant,
) {
    companion object {
        fun from(view: ReportView): ReportResDto =
            ReportResDto(
                view.id,
                view.articleId,
                view.title,
                view.revision,
                view.reason,
                view.reporterUserId,
                view.reportedAt,
            )
    }
}
