package io.github.wntopia.gikipedia.server.domain.history.service

import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.domain.history.dto.ArticleHistorySegmentEntry
import io.github.wntopia.gikipedia.server.domain.history.entity.ArticleHistorySegmentJpaEntity
import io.github.wntopia.gikipedia.server.domain.history.repository.ArticleHistoryRepository
import io.github.wntopia.gikipedia.server.domain.history.repository.ArticleHistorySegmentRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 두 스냅샷 사이의 interior 리비전들을 압축 세그먼트 하나로 묶는다.
 *
 * 호출은 [ArticleHistoryCompactionScheduler]에서만 이루어진다(편집 요청 경로와는 완전히 분리).
 */
@Component
class ArticleHistoryCompactionService(
    private val articleRepository: ArticleRepository,
    private val articleHistoryRepository: ArticleHistoryRepository,
    private val articleHistorySegmentRepository: ArticleHistorySegmentRepository,
    private val codec: ArticleHistorySegmentCodec,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * (fromRevision, toRevision) 스냅샷 구간의 interior 리비전들을 압축 세그먼트 하나로 묶는다.
     *
     * 세그먼트 저장 → 원본 삭제 순서를 반드시 지킨다. 저장이 unique 제약 위반 등으로 실패하면 트랜잭션이
     * 롤백되어 삭제는 전혀 일어나지 않는다 — 다중 인스턴스가 같은 구간을 동시에 압축 시도해도 안전하다.
     */
    @Transactional
    fun compactSegment(
        articleId: Long,
        fromRevision: Int,
        toRevision: Int,
    ) {
        if (toRevision - fromRevision <= 1) return

        if (articleHistorySegmentRepository.existsByArticleIdAndFromRevision(articleId, fromRevision)) {
            log.debug("이미 압축된 세그먼트, 스킵 (articleId={}, fromRevision={})", articleId, fromRevision)
            return
        }

        val interiorHistories =
            articleHistoryRepository.findByArticleIdAndRevisionBetweenOrderByRevisionAsc(
                articleId,
                fromRevision + 1,
                toRevision - 1,
            )

        val expectedCount = toRevision - fromRevision - 1
        if (interiorHistories.size != expectedCount) {
            log.warn(
                "interior 리비전 개수 불일치, 압축 스킵 (articleId={}, from={}, to={}, expected={}, actual={})",
                articleId,
                fromRevision,
                toRevision,
                expectedCount,
                interiorHistories.size,
            )
            return
        }

        val entries =
            interiorHistories.map {
                ArticleHistorySegmentEntry(
                    revision = it.revision,
                    editor = it.editor,
                    diff = it.diff,
                    createdAt = requireNotNull(it.createdAt) { "history.createdAt이 null입니다 (id=${it.id})" },
                )
            }

        val articleRef = articleRepository.getReferenceById(articleId)
        articleHistorySegmentRepository.save(
            ArticleHistorySegmentJpaEntity(articleRef, fromRevision, toRevision, codec.encode(entries)),
        )

        val deletedCount = articleHistoryRepository.deleteInteriorRevisions(articleId, fromRevision + 1, toRevision - 1)

        log.info(
            "히스토리 세그먼트 압축 완료 (articleId={}, from={}, to={}, interior={}, deleted={})",
            articleId,
            fromRevision,
            toRevision,
            entries.size,
            deletedCount,
        )
    }
}
