package io.github.wntopia.gikipedia.server.domain.history.service.impl

import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.domain.history.dto.response.ArticleRevisionResDto
import io.github.wntopia.gikipedia.server.domain.history.dto.response.ArticleRevisionSummaryResDto
import io.github.wntopia.gikipedia.server.domain.history.entity.ArticleSnapshotJpaEntity
import io.github.wntopia.gikipedia.server.domain.history.repository.ArticleHistoryRepository
import io.github.wntopia.gikipedia.server.domain.history.repository.ArticleHistorySegmentRepository
import io.github.wntopia.gikipedia.server.domain.history.repository.ArticleSnapshotRepository
import io.github.wntopia.gikipedia.server.domain.history.service.ArticleHistorySegmentCodec
import io.github.wntopia.gikipedia.server.domain.history.service.ReconstructArticleService
import io.github.wntopia.gikipedia.server.global.diff.ArticleDiff
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import team.themoment.sdk.exception.ExpectedException
import java.time.Instant

@Service
class ReconstructArticleServiceImpl(
    private val articleRepository: ArticleRepository,
    private val articleHistoryRepository: ArticleHistoryRepository,
    private val articleSnapshotRepository: ArticleSnapshotRepository,
    private val articleHistorySegmentRepository: ArticleHistorySegmentRepository,
    private val articleHistorySegmentCodec: ArticleHistorySegmentCodec,
    private val articleDiff: ArticleDiff,
) : ReconstructArticleService {
    /**
     * 재구성 결과를 캐시한다. 과거 리비전은 불변이므로 캐시가 영구 유효하다. 최신 리비전(unless)은 이후 수정으로 바뀔 수 있어 캐시하지 않는다 — 최신 문서는 항상
     * [io.github.wntopia.gikipedia.server.domain.article.service.QueryArticleService]로 조회하는 것을 전제로 한다.
     */
    @Transactional(readOnly = true)
    @Cacheable(
        cacheNames = [ARTICLE_REVISION_CACHE],
        key = "#articleId + ':' + #revision",
        unless = "#result.latest",
    )
    override fun reconstruct(
        articleId: Long,
        revision: Int,
    ): ArticleRevisionResDto {
        val article =
            articleRepository
                .findById(articleId)
                .orElseThrow { ExpectedException("존재하지 않는 게시글입니다.", HttpStatus.NOT_FOUND) }

        // 최신 리비전 여부는 압축 대상이 아닌 open tail의 row로 판별하므로, 압축 여부와 무관하게 항상 정확하다.
        // 이 값이 없거나 요청 리비전이 [1, latestRevision] 범위를 벗어나면 애초에 존재하지 않는 리비전이다.
        val latestRevision = articleHistoryRepository.findTopByArticleIdOrderByRevisionDesc(articleId)?.revision
        if (latestRevision == null || revision < 1 || revision > latestRevision) {
            throw ExpectedException("존재하지 않는 리비전입니다.", HttpStatus.NOT_FOUND)
        }

        val snapshot =
            articleSnapshotRepository
                .findTopByArticleIdAndRevisionLessThanEqualOrderByRevisionDesc(articleId, revision)
                ?: throw ExpectedException("스냅샷이 없어 문서를 재구성할 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR)

        val resolved = resolveRevision(articleId, snapshot, revision)

        return ArticleRevisionResDto(
            articleId = articleId,
            revision = revision,
            title = article.title,
            content = resolved.content,
            editor = resolved.editor,
            editedAt = resolved.editedAt,
            latest = revision == latestRevision,
        )
    }

    /**
     * 스냅샷 이후 diff를 순차 적용해 content를 복원하고, 대상 리비전의 editor/createdAt을 함께 찾는다.
     *
     * interior 구간은 [ArticleHistoryCompactionService]에 의해 원자적으로(세그먼트 저장 + 원본 삭제가
     * 하나의 트랜잭션) 압축되므로, 요청 범위의 raw row는 "전부 존재" 또는 "전부 압축되어 없음" 두 상태만
     * 가능하다 — 일부만 압축된 중간 상태는 발생하지 않는다.
     */
    private fun resolveRevision(
        articleId: Long,
        snapshot: ArticleSnapshotJpaEntity,
        revision: Int,
    ): ResolvedRevision {
        if (revision == snapshot.revision) {
            // 스냅샷 경계 리비전 — diff 적용이 필요 없고, 이 row는 압축 대상이 아니라 항상 raw로 남아있다.
            val target =
                articleHistoryRepository.findByArticleIdAndRevision(articleId, revision)
                    ?: throw ExpectedException("존재하지 않는 리비전입니다.", HttpStatus.NOT_FOUND)
            return ResolvedRevision(snapshot.content, target.editor, target.createdAt)
        }

        val neededCount = revision - snapshot.revision
        val rawDiffs =
            articleHistoryRepository.findByArticleIdAndRevisionBetweenOrderByRevisionAsc(
                articleId,
                snapshot.revision + 1,
                revision,
            )

        if (rawDiffs.size == neededCount) {
            val content = rawDiffs.fold(snapshot.content) { acc, history -> articleDiff.apply(acc, history.diff) }
            val target = rawDiffs.last()
            return ResolvedRevision(content, target.editor, target.createdAt)
        }

        // raw row가 부족함 = 해당 interior 구간이 이미 압축됨. 세그먼트에서 복원한다.
        val segment =
            articleHistorySegmentRepository.findByArticleIdAndFromRevision(articleId, snapshot.revision)
                ?: throw ExpectedException("존재하지 않는 리비전입니다.", HttpStatus.NOT_FOUND)
        val entries = articleHistorySegmentCodec.decode(segment.compressedPayload)
        val target =
            entries.find { it.revision == revision }
                ?: throw ExpectedException("존재하지 않는 리비전입니다.", HttpStatus.NOT_FOUND)
        val content =
            entries
                .filter { it.revision <= revision }
                .fold(snapshot.content) { acc, entry -> articleDiff.apply(acc, entry.diff) }
        return ResolvedRevision(content, target.editor, target.createdAt)
    }

    @Transactional(readOnly = true)
    override fun listRevisions(articleId: Long): List<ArticleRevisionSummaryResDto> {
        if (!articleRepository.existsById(articleId)) {
            throw ExpectedException("존재하지 않는 게시글입니다.", HttpStatus.NOT_FOUND)
        }

        val rawSummaries =
            articleHistoryRepository
                .findByArticleIdOrderByRevisionDesc(articleId)
                .map(ArticleRevisionSummaryResDto::from)

        // TODO(성능 최적화, 후속 과제): 지금은 정확성 우선으로 세그먼트를 매번 풀어서 병합한다. 문서당
        // 세그먼트 개수가 늘어나면 메타데이터(revision/editor/createdAt)만 압축하지 않는 별도 저장 방식으로
        // 전환하는 것을 고려한다.
        val segmentSummaries =
            articleHistorySegmentRepository.findByArticleId(articleId).flatMap { segment ->
                articleHistorySegmentCodec.decode(segment.compressedPayload).map {
                    ArticleRevisionSummaryResDto(revision = it.revision, editor = it.editor, editedAt = it.createdAt)
                }
            }

        return (rawSummaries + segmentSummaries).sortedByDescending { it.revision }
    }

    private data class ResolvedRevision(
        val content: String,
        val editor: String,
        val editedAt: Instant?,
    )

    companion object {
        const val ARTICLE_REVISION_CACHE = "articleRevision"
    }
}
