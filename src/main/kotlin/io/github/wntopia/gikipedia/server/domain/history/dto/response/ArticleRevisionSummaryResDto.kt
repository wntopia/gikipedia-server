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
    @field:Schema(description = "편집 요약 (없으면 null)", example = "오탈자 수정")
    val summary: String? = null,
    @field:Schema(description = "가벼운 수정 여부")
    val minorEdit: Boolean = false,
    @field:Schema(description = "직전 리비전 대비 글자 수 변화 (추가면 양수, 삭제면 음수. 이 기능 이전 리비전은 null)", example = "-12")
    val charDelta: Int? = null,
) {
    companion object {
        fun from(entity: ArticleHistoryJpaEntity): ArticleRevisionSummaryResDto =
            ArticleRevisionSummaryResDto(
                revision = entity.revision,
                editor = entity.editor,
                editedAt = entity.createdAt,
                summary = entity.summary,
                minorEdit = entity.minorEdit,
                charDelta = entity.charDelta,
            )
    }
}
