package io.github.wntopia.gikipedia.server.domain.link.service

import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.domain.link.repository.ArticleLinkRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import kotlin.concurrent.thread

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

    /** 기동을 막지 않도록 별도 스레드에서 돌린다 — 문서가 많으면 오래 걸리고, 그동안 서비스는 정상 응답해야 한다. */
    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReady() {
        thread(name = "article-link-backfill", isDaemon = true) { backfillIfEmpty() }
    }

    fun backfillIfEmpty() {
        if (articleLinkRepository.count() > 0) return

        var lastId = 0L
        var indexed = 0
        while (true) {
            val ids = articleRepository.findIdsAfter(lastId, PageRequest.of(0, CHUNK_SIZE))
            if (ids.isEmpty()) break
            ids.forEach { id -> backfillOne(id) }
            indexed += ids.size
            lastId = ids.last()
        }
        log.info("문서 링크 관계 백필 완료 (articles={})", indexed)
    }

    /** 한 문서의 실패가 전체 백필을 멈추지 않도록 로그만 남기고 다음 문서로 넘어간다. */
    private fun backfillOne(id: Long) {
        try {
            articleRepository.findById(id).ifPresent { articleLinkIndexer.reindex(id, it.title, it.content) }
        } catch (e: Exception) {
            log.warn("문서 링크 백필 실패, 다음 문서로 진행 (articleId={})", id, e)
        }
    }

    companion object {
        private const val CHUNK_SIZE = 100
    }
}
