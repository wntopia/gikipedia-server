package io.github.wntopia.gikipedia.server.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 재구성 캐시(Caffeine) 튜닝 값. `application.yaml`의 `app.cache.article-revision` 하위에서 조정한다.
 *
 * 하드코딩 상수가 아니라 프로퍼티로 외부화했으므로 캐시 히트/미스 지표를 보고 재컴파일 없이 크기·TTL을 조정할 수 있다. 프로파일별 `application-{profile}.yaml`에서 값을 오버라이드하면 환경별 전략도
 * 구성할 수 있다.
 */
@ConfigurationProperties(prefix = "app.cache.article-revision")
data class CacheEnvironment(
    /** 캐시에 보관할 최대 엔트리 수. 초과 시 Caffeine이 정책에 따라 방출한다. */
    val maxEntries: Long = 10_000,
    /** 마지막 접근 이후 만료까지의 시간(시간 단위). 과거 리비전은 불변이라 무효화가 아니라 이 TTL로만 관리된다. */
    val ttlHours: Long = 6,
)
