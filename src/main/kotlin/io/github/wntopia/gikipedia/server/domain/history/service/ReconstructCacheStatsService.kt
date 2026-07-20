package io.github.wntopia.gikipedia.server.domain.history.service

import com.github.benmanes.caffeine.cache.Cache
import io.github.wntopia.gikipedia.server.domain.history.dto.response.CacheStatsResDto
import io.github.wntopia.gikipedia.server.domain.history.service.impl.ReconstructArticleServiceImpl
import org.springframework.cache.CacheManager
import org.springframework.cache.caffeine.CaffeineCache
import org.springframework.stereotype.Service

/** 재구성 캐시(Caffeine)의 히트/미스 통계를 노출한다. recordStats가 켜져 있어야 의미 있는 값이 나온다. */
@Service
class ReconstructCacheStatsService(
    private val cacheManager: CacheManager,
) {
    fun stats(): CacheStatsResDto {
        val cache =
            cacheManager.getCache(ReconstructArticleServiceImpl.ARTICLE_REVISION_CACHE) as? CaffeineCache
                ?: return EMPTY

        @Suppress("UNCHECKED_CAST")
        val nativeCache = cache.nativeCache as Cache<Any, Any>
        val stats = nativeCache.stats()
        return CacheStatsResDto(
            hitCount = stats.hitCount(),
            missCount = stats.missCount(),
            hitRate = stats.hitRate(),
            evictionCount = stats.evictionCount(),
            estimatedSize = nativeCache.estimatedSize(),
        )
    }

    companion object {
        private val EMPTY = CacheStatsResDto(0, 0, 0.0, 0, 0)
    }
}
