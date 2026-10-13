package io.github.wntopia.gikipedia.server.domain.user.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

data class ContributionStatsResDto(
    @field:Schema(description = "가입일 (첫 로그인 시각, ISO-8601, UTC)")
    val joinedAt: Instant?,
    @field:Schema(description = "총 편집 수 (기존 문서를 수정한 횟수. 문서 생성은 포함하지 않는다)", example = "42")
    val totalEdits: Long,
    @field:Schema(description = "새로 만든 문서 수", example = "3")
    val createdArticles: Long,
    @field:Schema(description = "오늘 편집 수 (한국 시간 기준 오늘 0시부터)", example = "2")
    val todayEdits: Long,
)
