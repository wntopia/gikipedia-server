package io.github.wntopia.gikipedia.server.global.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "article.search")
data class ArticleSearchEnvironment(
    val maxTime: Duration = Duration.ofSeconds(2),
) {
    init {
        require(maxTime.toMillis() > 0) { "article.search.max-time은 1ms 이상이어야 합니다." }
    }
}
