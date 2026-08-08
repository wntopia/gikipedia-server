package io.github.wntopia.gikipedia.server.domain.history.service

import io.github.wntopia.gikipedia.server.domain.history.repository.ArticleSnapshotRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 두 스냅샷 사이의 interior 리비전들을 매일 밤 압축해 앞쪽 스냅샷 row에 붙이는 배치.
 *
 * [io.github.wntopia.gikipedia.server.domain.article.service.ArticleConsistencyCheckScheduler](04:00)와
 * 겹치지 않도록 30분 뒤에 돈다. 편집 요청 경로와 완전히 분리되어 있으며, 원본 row를 삭제하는 파괴적
 * 작업이라 `article.history.compaction.enabled` 플래그로 즉시 끌 수 있게 해둔다.
 */
@Component
class ArticleHistoryCompactionScheduler(
    private val articleSnapshotRepository: ArticleSnapshotRepository,
    private val articleHistoryCompactionService: ArticleHistoryCompactionService,
    @param:Value("\${article.history.compaction.enabled:true}") private val enabled: Boolean,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 30 4 * * *")
    fun compact() {
        if (!enabled) {
            log.info("히스토리 압축 배치 비활성화 상태, 스킵")
            return
        }

        // 배치 전체가 하나의 방어되지 않은 예외로 조용히 중단되지 않도록 감싼다 — 탐색 쿼리 자체가
        // 실패해도 실패 사실이 로그로 남는 게 중요하다.
        try {
            val snapshotsByArticle =
                articleSnapshotRepository
                    .findAllRevisionsOrderByArticleAscRevisionAsc()
                    .groupBy({ it.getArticleId() }, { it.getRevision() to it.getCompacted() })

            var candidateCount = 0
            var successCount = 0
            snapshotsByArticle.forEach { (articleId, revisions) ->
                revisions.sortedBy { it.first }.zipWithNext().forEach { (from, to) ->
                    val (fromRevision, fromCompacted) = from
                    val (toRevision, _) = to
                    if (toRevision - fromRevision <= 1) return@forEach
                    if (fromCompacted) return@forEach

                    candidateCount++
                    try {
                        articleHistoryCompactionService.compactSegment(articleId, fromRevision, toRevision)
                        successCount++
                    } catch (e: Exception) {
                        log.error(
                            "구간 압축 실패, 다음 구간은 계속 진행 (articleId={}, from={}, to={})",
                            articleId,
                            fromRevision,
                            toRevision,
                            e,
                        )
                    }
                }
            }
            log.info("히스토리 압축 배치 완료 (후보={}, 성공={})", candidateCount, successCount)
        } catch (e: Exception) {
            log.error("히스토리 압축 배치 실행 중 실패 — 이번 회차는 압축이 수행되지 않았다", e)
        }
    }
}
