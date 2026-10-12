package io.github.wntopia.gikipedia.server.domain.article.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

data class RecentChangeResDto(
    @field:Schema(description = "문서 ID", example = "1")
    val articleId: Long,
    @field:Schema(description = "문서 제목")
    val title: String,
    @field:Schema(description = "마지막으로 기여한 사용자의 이름 (비로그인으로 만든 문서처럼 알 수 없으면 null)", example = "이서연")
    val lastEditorName: String?,
    @field:Schema(description = "마지막 수정 시각 (ISO-8601, UTC)")
    val updatedAt: Instant,
    @field:Schema(description = "새로 만든 문서인지 여부 (생성 이후 아직 수정되지 않았으면 true)")
    val newlyCreated: Boolean,
    @field:Schema(description = "오늘(한국 시간 0시부터) 이 문서를 수정한 횟수", example = "5")
    val todayEditCount: Long,
    @field:Schema(description = "오늘 이 문서를 수정한 서로 다른 사용자 수. \"○○ 외 N명\"의 N은 이 값에서 1을 뺀 값이다.", example = "4")
    val todayEditorCount: Long,
)
