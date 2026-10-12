package io.github.wntopia.gikipedia.server.domain.link.service

import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.domain.link.repository.ArticleLinkRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

/**
 * 이 기능 배포 전부터 있던 문서들의 링크 관계를 한 번 채운다. `article_links`가 비어 있을 때만 돌고, 이후 변경은
 * 문서 저장 경로의 [ArticleLinkIndexer] 호출이 유지한다. 문서마다 별도 트랜잭션이라 중간에 죽어도 이미 채운
 * 문서는 남고, 재시작하면 (테이블이 비어 있지 않으므로) 이어서 채우지는 않는다 — 그 경우 비운 뒤 다시 기동한다.
 */
@Component
class ArticleLinkBackfillRunner(
    private val articleRepository: ArticleRepository,
    private val articleLinkRepository: ArticleLinkRepository,
    private val articleLinkIndexer: ArticleLinkIndexer,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener(ApplicationReadyEvent::class)
    fun backfillIfEmpty() {
        if (articleLinkRepository.count() > 0) return

        var lastId = 0L
        var indexed = 0
        while (true) {
            val ids = articleRepository.findIdsAfter(lastId, PageRequest.of(0, CHUNK_SIZE))
            if (ids.isEmpty()) break
            ids.forEach { id ->
                articleRepository.findById(id).ifPresent { articleLinkIndexer.reindex(id, it.title, it.content) }
            }
            indexed += ids.size
            lastId = ids.last()
        }
        log.info("문서 링크 관계 백필 완료 (articles={})", indexed)
    }

    companion object {
        private const val CHUNK_SIZE = 100
    }
}
