package io.github.wntopia.gikipedia.server.domain.history.dto.response

import io.github.wntopia.gikipedia.server.domain.history.entity.ArticleHistoryJpaEntity
import java.time.Instant

/** 버전 히스토리 목록의 한 항목. 내용 없이 리비전 메타데이터만 담는다. */
data class ArticleRevisionSummaryResDto(
    val revision: Int,
    val editor: String,
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
