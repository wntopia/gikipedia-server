package io.github.wntopia.gikipedia.server.domain.article.service

import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate

/**
 * Redis에 쌓인 조회수 증가분을 주기적으로 MySQL `articles.view_count`에 반영한다.
 *
 * 한 트랜잭션으로 모두 반영한 뒤에 증가분을 지운다. 반영 직후 지우기 전에 프로세스가 죽으면 다음 실행에서 같은
 * 증가분을 한 번 더 반영할 수 있다(최대 한 주기 분량의 과집계 — 유실보다 낫다고 보고 허용한다).
 */
@Component
class ArticleViewFlushScheduler(
    private val articleViewCounter: ArticleViewCounter,
    private val articleRepository: ArticleRepository,
    private val transactionTemplate: TransactionTemplate,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${article.views.flush-interval-ms:30000}")
    fun flush() {
        try {
            val deltas = articleViewCounter.drainPending()
            if (deltas.isEmpty()) return

            transactionTemplate.executeWithoutResult {
                deltas.forEach { (articleId, delta) -> articleRepository.addViewCount(articleId, delta) }
            }
            articleViewCounter.confirmDrained()
            log.debug("조회수 반영 완료 (articles={})", deltas.size)
        } catch (e: Exception) {
            // 증가분은 processing 키에 남아 있어 다음 실행에서 이어서 반영된다.
            log.warn("조회수 반영 실패 — 다음 주기에 다시 시도한다", e)
        }
    }
}
