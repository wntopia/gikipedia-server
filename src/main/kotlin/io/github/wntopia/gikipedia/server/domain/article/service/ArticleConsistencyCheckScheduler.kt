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
    private val articleMongoSynchronizer: ArticleMongoSynchronizer,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 4 * * *")
    fun checkAndRepair() {
        // 배치 전체가 하나의 방어되지 않은 예외로 조용히 중단되지 않도록 감싼다 — 이 배치는
        // "마지막 안전망"이라, 실패했을 때 아무 흔적 없이 그냥 안 도는 것보다는 실패 사실이 로그로
        // 남는 게 중요하다.
        try {
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
                    repair(articleId, trueRevision)
                }
            }

            log.info("일일 정합성 검사 완료 (대상={}, 불일치={})", allArticleIds.size, mismatchCount)
        } catch (e: Exception) {
            log.error("일일 정합성 검사 배치 실행 중 실패 — 이번 회차는 복구가 수행되지 않았다", e)
        }
    }

    /**
     * article 1건 복구. ArticleMongoSynchronizer가 더 이상 MySQL을 재조회하지 않으므로, 복구에 필요한
     * 데이터를 여기서 직접 조회해 채운다. 개별 article 복구가 실패해도(예외/null 데이터) 로그만 남기고
     * 다음 article 복구를 계속 진행한다 — 한 문서의 문제로 그날 밤 전체 복구가 멈추지 않도록 하기 위함이다.
     */
    private fun repair(
        articleId: Long,
        trueRevision: Int,
    ) {
        try {
            val article = articleRepository.findById(articleId).orElse(null)
            if (article == null) {
                log.warn("복구 대상 article을 MySQL에서 찾을 수 없음, 건너뜀 (articleId={})", articleId)
                return
            }
            val createdAt = article.createdAt
            val updatedAt = article.updatedAt
            if (createdAt == null || updatedAt == null) {
                log.warn("복구 대상 article의 createdAt/updatedAt이 비어 있음, 건너뜀 (articleId={})", articleId)
                return
            }

            articleMongoSynchronizer.sync(
                articleId = articleId,
                revision = trueRevision,
                title = article.title,
                content = article.content,
                imageUrl = article.imageUrl,
                articleCreatedAt = createdAt,
                articleUpdatedAt = updatedAt,
            )
        } catch (e: Exception) {
            log.error("article {} 복구 중 실패, 다음 article은 계속 진행", articleId, e)
        }
    }

    companion object {
        private const val BASELINE_REVISION = 1
    }
}
