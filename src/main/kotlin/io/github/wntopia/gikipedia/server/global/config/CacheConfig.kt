package io.github.wntopia.gikipedia.server.global.config

import com.github.benmanes.caffeine.cache.Caffeine
import io.github.wntopia.gikipedia.server.domain.history.service.impl.ReconstructArticleServiceImpl
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

/**
 * 재구성된 과거 리비전 문서를 위한 Caffeine 캐시.
 *
 * 과거 리비전은 불변이라 무효화가 필요 없고 TTL만 둔다. 히트/미스 통계를 지표로 노출하기 위해 recordStats를 켠다. 다중 인스턴스 환경에서는 Redis로 전환할 수 있다(로드맵).
 */
@Configuration
@EnableCaching
class CacheConfig {
    @Bean
    fun cacheManager(): CacheManager {
        val manager = CaffeineCacheManager(ReconstructArticleServiceImpl.ARTICLE_REVISION_CACHE)
        manager.setCaffeine(
            Caffeine
                .newBuilder()
                .maximumSize(MAX_ENTRIES)
                .expireAfterAccess(Duration.ofHours(TTL_HOURS))
                .recordStats(),
        )
        return manager
    }

    companion object {
        private const val MAX_ENTRIES = 10_000L
        private const val TTL_HOURS = 6L
    }
}
