package io.github.wntopia.gikipedia.server.domain.report.dto.response

import io.github.wntopia.gikipedia.server.domain.report.entity.ReportStatus
import io.github.wntopia.gikipedia.server.domain.report.repository.ReportView
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

data class ReportResDto(
    @field:Schema(description = "신고 ID", example = "1")
    val id: Long,
    @field:Schema(description = "신고된 문서 ID", example = "1")
    val articleId: Long,
    @field:Schema(description = "신고된 문서 제목")
    val articleTitle: String,
    @field:Schema(description = "신고된 리비전 번호 (문서 전체를 신고했으면 null)")
    val revision: Int?,
    @field:Schema(description = "신고 사유")
    val reason: String,
    @field:Schema(description = "신고한 사용자의 계정 ID")
    val reporterUserId: Long,
    @field:Schema(description = "처리 상태 (OPEN: 접수됨, RESOLVED: 처리 완료)")
    val status: ReportStatus,
    @field:Schema(description = "접수 시각 (ISO-8601, UTC)")
    val createdAt: Instant,
) {
    companion object {
        fun from(view: ReportView): ReportResDto =
            ReportResDto(
                view.id,
                view.articleId,
                view.articleTitle,
                view.revision,
                view.reason,
                view.reporterUserId,
                view.status,
                view.createdAt,
            )
    }
}
