package io.github.wntopia.gikipedia.server.domain.history.dto.response

/** 재구성 캐시의 히트/미스 통계. 캐시 전략의 효과를 관찰하기 위한 지표. */
data class CacheStatsResDto(
    val hitCount: Long,
    val missCount: Long,
    val hitRate: Double,
    val evictionCount: Long,
    val estimatedSize: Long,
)
