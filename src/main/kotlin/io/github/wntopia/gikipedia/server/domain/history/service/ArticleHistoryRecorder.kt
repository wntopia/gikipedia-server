package io.github.wntopia.gikipedia.server.domain.history.service

import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleJpaEntity
import io.github.wntopia.gikipedia.server.domain.history.entity.ArticleHistoryJpaEntity
import io.github.wntopia.gikipedia.server.domain.history.entity.ArticleSnapshotJpaEntity
import io.github.wntopia.gikipedia.server.domain.history.event.ArticleUpdatedEvent
import io.github.wntopia.gikipedia.server.domain.history.repository.ArticleHistoryRepository
import io.github.wntopia.gikipedia.server.domain.history.repository.ArticleSnapshotRepository
import io.github.wntopia.gikipedia.server.global.diff.ArticleDiff
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

/**
 * 문서 수정 시 히스토리(diff)와 스냅샷을 쌓고, 커밋 후 Mongo 동기화 이벤트를 발행한다.
 *
 * 반드시 호출 측(예: UpdateArticleService)의 트랜잭션 안에서 실행되어야 한다. diff append와 스냅샷 저장이 문서 갱신과 하나의 원자 단위로 커밋되어야 하기 때문이다.
 * 이벤트는 [ApplicationEventPublisher]로 발행하고, 실제 Mongo 반영은 AFTER_COMMIT 리스너가 담당한다.
 */
@Component
class ArticleHistoryRecorder(
    private val articleHistoryRepository: ArticleHistoryRepository,
    private val articleSnapshotRepository: ArticleSnapshotRepository,
    private val articleDiff: ArticleDiff,
    private val eventPublisher: ApplicationEventPublisher,
    @param:Value("\${article.snapshot.interval:10}") private val snapshotInterval: Int,
) {
    /**
     * 문서의 이전 내용(before)에서 새 내용(after)으로의 변경을 새 리비전으로 기록한다.
     *
     * 첫 수정이라 히스토리가 비어 있으면 생성 시점 내용(before)을 리비전 1(스냅샷)로 먼저 확정한 뒤, 새 내용을 리비전 2로 쌓는다. 이렇게 해야 어떤 리비전이든 "가장 가까운 스냅샷 + 이후 diff"로
     * 재구성할 수 있다. content 변화가 없으면(diff 없음) 새 리비전은 만들지 않되, 이미지 등 다른 필드만 바뀌었을
     * 수 있으므로(title은 수정 불가) 현재 리비전 그대로 Mongo 동기화 이벤트는 다시 발행한다 — 그래야 Mongo/Redis에
     * 반영된 이미지가 뒤처지지 않는다.
     *
     * @return 새로 기록된 리비전 번호. content 변화가 없으면 null.
     */
    fun record(
        article: ArticleJpaEntity,
        before: String,
        after: String,
        editor: String,
    ): Int? {
        val articleId = requireNotNull(article.id)
        val diff = articleDiff.generate(before, after)
        if (diff.isEmpty()) {
            val currentRevision =
                articleHistoryRepository.findTopByArticleIdOrderByRevisionDesc(articleId)?.revision ?: BASELINE_REVISION
            eventPublisher.publishEvent(buildEvent(article, articleId, currentRevision))
            return null
        }

        val lastRevision = articleHistoryRepository.findTopByArticleIdOrderByRevisionDesc(articleId)?.revision

        if (lastRevision == null) {
            // 최초 수정: 생성 시점 내용을 리비전 1 스냅샷으로 baseline 확정한다.
            appendHistory(article, BASELINE_REVISION, editor, articleDiff.generate("", before))
            saveSnapshot(article, BASELINE_REVISION, before)
        }

        val newRevision = (lastRevision ?: BASELINE_REVISION) + 1
        appendHistory(article, newRevision, editor, diff)
        if (isSnapshotRevision(newRevision)) {
            saveSnapshot(article, newRevision, after)
        }

        eventPublisher.publishEvent(buildEvent(article, articleId, newRevision))
        return newRevision
    }

    /**
     * article의 createdAt/updatedAt은 JPA 감사(@CreatedDate/@LastModifiedDate) 필드라 타입상 nullable이지만,
     * 이미 영속화된 엔티티라면 항상 채워져 있어야 한다. 비어 있으면(=감사 설정 누락 등) Mongo에 잘못된 값을
     * 조용히 흘려보내는 대신 여기서 바로 실패시켜 원인을 드러낸다.
     */
    private fun buildEvent(
        article: ArticleJpaEntity,
        articleId: Long,
        revision: Int,
    ): ArticleUpdatedEvent =
        ArticleUpdatedEvent(
            articleId = articleId,
            revision = revision,
            title = article.title,
            content = article.content,
            imageUrl = article.imageUrl,
            createdAt = requireNotNull(article.createdAt) { "article.createdAt이 null입니다 (articleId=$articleId)" },
            updatedAt = requireNotNull(article.updatedAt) { "article.updatedAt이 null입니다 (articleId=$articleId)" },
        )

    /** 리비전 1과 이후 snapshotInterval 간격마다 스냅샷을 남긴다(예: interval=10 → 1, 11, 21...). */
    private fun isSnapshotRevision(revision: Int): Boolean = revision % snapshotInterval == 1

    private fun appendHistory(
        article: ArticleJpaEntity,
        revision: Int,
        editor: String,
        diff: String,
    ) {
        articleHistoryRepository.save(ArticleHistoryJpaEntity(article, revision, editor, diff))
    }

    private fun saveSnapshot(
        article: ArticleJpaEntity,
        revision: Int,
        content: String,
    ) {
        articleSnapshotRepository.save(ArticleSnapshotJpaEntity(article, revision, content))
    }

    companion object {
        private const val BASELINE_REVISION = 1
    }
}
