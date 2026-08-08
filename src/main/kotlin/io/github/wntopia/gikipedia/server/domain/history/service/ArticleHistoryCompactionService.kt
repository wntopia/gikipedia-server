package io.github.wntopia.gikipedia.server.domain.history.service

import io.github.wntopia.gikipedia.server.domain.history.dto.ArticleHistorySegmentEntry
import io.github.wntopia.gikipedia.server.domain.history.repository.ArticleHistoryRepository
import io.github.wntopia.gikipedia.server.domain.history.repository.ArticleSnapshotRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 두 스냅샷 사이의 interior 리비전들을 압축해, 앞쪽(from) 스냅샷 row에 페이로드로 붙인다.
 *
 * 호출은 [ArticleHistoryCompactionScheduler]에서만 이루어진다(편집 요청 경로와는 완전히 분리).
 */
@Component
class ArticleHistoryCompactionService(
    private val articleHistoryRepository: ArticleHistoryRepository,
    private val articleSnapshotRepository: ArticleSnapshotRepository,
    private val codec: ArticleHistorySegmentCodec,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * (fromRevision, toRevision) 스냅샷 구간의 interior 리비전들을 압축해 fromRevision 스냅샷 row에 붙이고,
     * 원본 interior row를 삭제한다.
     *
     * "페이로드 부착 → 원본 삭제" 순서를 반드시 지킨다. 이 메서드 전체가 하나의 트랜잭션이라 중간에 실패하면
     * 삭제까지 포함해 전부 롤백된다.
     */
    @Transactional
    fun compactSegment(
        articleId: Long,
        fromRevision: Int,
        toRevision: Int,
    ) {
        if (toRevision - fromRevision <= 1) return

        val fromSnapshot = articleSnapshotRepository.findByArticleIdAndRevision(articleId, fromRevision)
        if (fromSnapshot == null) {
            log.warn("압축 대상 스냅샷을 찾을 수 없음, 스킵 (articleId={}, fromRevision={})", articleId, fromRevision)
            return
        }
        if (fromSnapshot.compressedInteriorPayload != null) {
            log.debug("이미 압축된 구간, 스킵 (articleId={}, fromRevision={})", articleId, fromRevision)
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

        fromSnapshot.attachCompressedInteriorPayload(codec.encode(entries))
        articleSnapshotRepository.save(fromSnapshot)

        val deletedCount = articleHistoryRepository.deleteInteriorRevisions(articleId, fromRevision + 1, toRevision - 1)

        log.info(
            "히스토리 구간 압축 완료 (articleId={}, from={}, to={}, interior={}, deleted={})",
            articleId,
            fromRevision,
            toRevision,
            entries.size,
            deletedCount,
        )
    }
}
