package io.github.wntopia.gikipedia.server.domain.history.service.impl

import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.domain.history.dto.response.ArticleRevisionResDto
import io.github.wntopia.gikipedia.server.domain.history.dto.response.ArticleRevisionSummaryResDto
import io.github.wntopia.gikipedia.server.domain.history.repository.ArticleHistoryRepository
import io.github.wntopia.gikipedia.server.domain.history.repository.ArticleSnapshotRepository
import io.github.wntopia.gikipedia.server.domain.history.service.ReconstructArticleService
import io.github.wntopia.gikipedia.server.global.diff.ArticleDiff
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import team.themoment.sdk.exception.ExpectedException

@Service
class ReconstructArticleServiceImpl(
    private val articleRepository: ArticleRepository,
    private val articleHistoryRepository: ArticleHistoryRepository,
    private val articleSnapshotRepository: ArticleSnapshotRepository,
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

        val histories = articleHistoryRepository.findByArticleIdOrderByRevisionDesc(articleId)
        val targetHistory =
            histories.find { it.revision == revision }
                ?: throw ExpectedException("존재하지 않는 리비전입니다.", HttpStatus.NOT_FOUND)
        val latestRevision = histories.firstOrNull()?.revision ?: revision

        val snapshot =
            articleSnapshotRepository
                .findTopByArticleIdAndRevisionLessThanEqualOrderByRevisionDesc(articleId, revision)
                ?: throw ExpectedException("스냅샷이 없어 문서를 재구성할 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR)

        val diffs =
            articleHistoryRepository.findByArticleIdAndRevisionBetweenOrderByRevisionAsc(
                articleId,
                snapshot.revision + 1,
                revision,
            )
        val content = diffs.fold(snapshot.content) { acc, history -> articleDiff.apply(acc, history.diff) }

        return ArticleRevisionResDto(
            articleId = articleId,
            revision = revision,
            title = article.title,
            content = content,
            editor = targetHistory.editor,
            editedAt = targetHistory.createdAt,
            latest = revision == latestRevision,
        )
    }

    @Transactional(readOnly = true)
    override fun listRevisions(articleId: Long): List<ArticleRevisionSummaryResDto> {
        if (!articleRepository.existsById(articleId)) {
            throw ExpectedException("존재하지 않는 게시글입니다.", HttpStatus.NOT_FOUND)
        }
        return articleHistoryRepository
            .findByArticleIdOrderByRevisionDesc(articleId)
            .map(ArticleRevisionSummaryResDto::from)
    }

    companion object {
        const val ARTICLE_REVISION_CACHE = "articleRevision"
    }
}
