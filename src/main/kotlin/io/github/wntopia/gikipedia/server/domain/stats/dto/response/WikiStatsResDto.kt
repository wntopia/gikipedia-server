package io.github.wntopia.gikipedia.server.domain.stats.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class WikiStatsResDto(
    @field:Schema(description = "전체 문서 수", example = "128")
    val totalArticles: Long,
    @field:Schema(description = "이번 주(월요일 0시, 한국 시간 기준) 이후의 편집 수. 문서 생성과 수정을 모두 센다.", example = "37")
    val weeklyEdits: Long,
    @field:Schema(description = "가입 사용자 수 (한 번이라도 로그인한 사용자)", example = "54")
    val registeredUsers: Long,
)
