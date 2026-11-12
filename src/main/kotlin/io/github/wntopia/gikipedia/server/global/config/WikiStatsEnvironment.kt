package io.github.wntopia.gikipedia.server.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

/** 위키 통계 API 튜닝 값. `application.yaml`의 `app.stats` 하위에서 조정한다. */
@ConfigurationProperties(prefix = "app.stats")
data class WikiStatsEnvironment(
    /** 집계 결과를 재사용하는 시간(초). 이 시간 안에는 같은 값을 돌려주므로 문서 변경 반영도 최대 이만큼 늦어진다. */
    val cacheSeconds: Long = 60,
)
