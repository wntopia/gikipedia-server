package io.github.wntopia.gikipedia.server.domain.history.dto.response

import io.swagger.v3.oas.annotations.media.Schema

/** 재구성 캐시의 히트/미스 통계. 캐시 전략의 효과를 관찰하기 위한 지표. */
data class CacheStatsResDto(
    @field:Schema(description = "캐시 히트 횟수")
    val hitCount: Long,
    @field:Schema(description = "캐시 미스 횟수")
    val missCount: Long,
    @field:Schema(description = "캐시 히트율 (0.0 ~ 1.0)")
    val hitRate: Double,
    @field:Schema(description = "캐시에서 제거된 항목 수")
    val evictionCount: Long,
    @field:Schema(description = "현재 캐시에 저장된 항목 수 (추정치)")
    val estimatedSize: Long,
)
