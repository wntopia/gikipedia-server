package io.github.wntopia.gikipedia.server.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 조회수 집계 튜닝 값. `application.yaml`의 `article.views` 하위에서 조정한다. Redis → MySQL 반영 주기는
 * `article.views.flush-interval-ms`(기본 30초)로 [io.github.wntopia.gikipedia.server.domain.article.service.ArticleViewFlushScheduler] 가 직접 읽는다.
 */
@ConfigurationProperties(prefix = "article.views")
data class ArticleViewEnvironment(
    /** 같은 사용자가 같은 문서를 다시 열어도 조회수를 올리지 않는 시간(분). */
    val dedupeMinutes: Long = 30,
)
