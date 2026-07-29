package io.github.wntopia.gikipedia.server.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 조회 응답을 Redis에 캐싱할 때 쓰는 TTL. `application.yaml`의 `article.cache` 하위에서 조정한다.
 *
 * 이 TTL은 "주 갱신 경로"가 아니라, 능동 갱신(수정/생성 시 즉시 SET)이 어떤 이유로든 실패했을 때를 위한
 * 보험이다. 매 쓰기마다 캐시가 새로 채워지므로 TTL을 짧게 잡을 이유가 없다 — 오히려 짧으면 캐시 히트율만
 * 깎인다.
 */
@ConfigurationProperties(prefix = "article.cache")
data class ArticleCacheEnvironment(
    val ttlMinutes: Long = 60,
)
