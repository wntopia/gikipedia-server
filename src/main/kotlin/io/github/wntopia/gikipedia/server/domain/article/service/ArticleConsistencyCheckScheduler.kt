package io.github.wntopia.gikipedia.server.domain.article.service

import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleMongoRepository
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.domain.history.repository.ArticleHistoryRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * MySQL(정본)과 Mongo(복제본) 간 revision 불일치를 매일 새벽에 스캔해 자동 복구하는 보험 배치.
 *
 * 평시에는 생성/수정 이벤트가 곧바로 Mongo를 동기화하므로, 이 배치는 그걸 빠져나간 극단적 경우
 * (리스너 실패가 10분 백업 재시도로도 안 잡힌 경우 등)를 잡는 마지막 안전망이다.
 */
@Component
class ArticleConsistencyCheckScheduler(
    private val articleRepository: ArticleRepository,
    private val articleHistoryRepository: ArticleHistoryRepository,
    private val articleMongoRepository: ArticleMongoRepository,
    private val articleMongoSyncService: ArticleMongoSyncService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 4 * * *")
    fun checkAndRepair() {
        val allArticleIds = articleRepository.findAllIds()
        val trueRevisions =
            articleHistoryRepository
                .findLatestRevisionPerArticle()
                .associate { it.getArticleId() to it.getRevision() }
        val mongoRevisions = articleMongoRepository.findAll().associate { it.documentId to it.revision }

        var mismatchCount = 0
        allArticleIds.forEach { articleId ->
            // 한 번도 수정 안 된 문서는 article_histories에 행이 없다 — 그 경우 "생성 시점 그대로"인
            // 리비전 1이 정답이다(CreateArticleServiceImpl이 발행하는 baseline과 동일한 값).
            val trueRevision = trueRevisions[articleId] ?: BASELINE_REVISION
            val mongoRevision = mongoRevisions[articleId]

            if (mongoRevision == null || mongoRevision < trueRevision) {
                mismatchCount++
                log.warn(
                    "MySQL-Mongo 리비전 불일치 발견, 자동 복구 (articleId={}, mysql={}, mongo={})",
                    articleId,
                    trueRevision,
                    mongoRevision,
                )
                articleMongoSyncService.sync(articleId, trueRevision)
            }
        }

        log.info("일일 정합성 검사 완료 (대상={}, 불일치={})", allArticleIds.size, mismatchCount)
    }

    companion object {
        private const val BASELINE_REVISION = 1
    }
}
