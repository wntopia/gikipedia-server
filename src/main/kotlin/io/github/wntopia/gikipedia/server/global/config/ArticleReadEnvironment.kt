package io.github.wntopia.gikipedia.server.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

enum class ArticleReadSource {
    /** Redis → Mongo 순으로 조회하는 신규 경로. */
    REDIS_MONGO,

    /** 기존처럼 MySQL을 직접 조회하는 경로. */
    MYSQL,
}

/**
 * 문서 조회 API의 읽기 경로 스위치. `application.yaml`의 `article.read.source`에서 조정한다.
 *
 * Redis→Mongo 경로 전환에 문제가 생기면 재배포만으로 기존 MySQL 직접 조회로 되돌릴 수 있게 하는
 * 안전장치다. 단일 서버 규모라 재시작 없는 런타임 토글까지는 필요 없다고 판단했다.
 */
@ConfigurationProperties(prefix = "article.read")
data class ArticleReadEnvironment(
    val source: ArticleReadSource = ArticleReadSource.REDIS_MONGO,
)
