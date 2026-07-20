package io.github.wntopia.gikipedia.server.domain.history.controller

import io.github.wntopia.gikipedia.server.domain.history.dto.response.ArticleRevisionResDto
import io.github.wntopia.gikipedia.server.domain.history.dto.response.ArticleRevisionSummaryResDto
import io.github.wntopia.gikipedia.server.domain.history.dto.response.CacheStatsResDto
import io.github.wntopia.gikipedia.server.domain.history.service.ReconstructArticleService
import io.github.wntopia.gikipedia.server.domain.history.service.ReconstructCacheStatsService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/articles")
class ArticleHistoryController(
    private val reconstructArticleService: ReconstructArticleService,
    private val reconstructCacheStatsService: ReconstructCacheStatsService,
) {
    /** 특정 리비전 시점으로 재구성된 문서. */
    @GetMapping("/{articleId}/revisions/{revision}")
    fun getRevision(
        @PathVariable articleId: Long,
        @PathVariable revision: Int,
    ): ArticleRevisionResDto = reconstructArticleService.reconstruct(articleId, revision)

    /** 문서의 리비전 목록(최신 순). */
    @GetMapping("/{articleId}/revisions")
    fun listRevisions(
        @PathVariable articleId: Long,
    ): List<ArticleRevisionSummaryResDto> = reconstructArticleService.listRevisions(articleId)

    /** 재구성 캐시 히트/미스 통계. */
    @GetMapping("/revisions/cache-stats")
    fun cacheStats(): CacheStatsResDto = reconstructCacheStatsService.stats()
}
