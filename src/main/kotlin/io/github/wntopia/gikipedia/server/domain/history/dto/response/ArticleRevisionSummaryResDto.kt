package io.github.wntopia.gikipedia.server.domain.history.dto.response

import io.github.wntopia.gikipedia.server.domain.history.entity.ArticleHistoryJpaEntity
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

/** 버전 히스토리 목록의 한 항목. 내용 없이 리비전 메타데이터만 담는다. */
data class ArticleRevisionSummaryResDto(
    @field:Schema(description = "리비전 번호", example = "3")
    val revision: Int,
    @field:Schema(description = "편집자 (\"학번 이름\")", example = "2412 홍길동")
    val editor: String,
    @field:Schema(description = "편집 시각 (ISO-8601, UTC)")
    val editedAt: Instant?,
) {
    companion object {
        fun from(entity: ArticleHistoryJpaEntity): ArticleRevisionSummaryResDto =
            ArticleRevisionSummaryResDto(
                revision = entity.revision,
                editor = entity.editor,
                editedAt = entity.createdAt,
            )
    }
}
