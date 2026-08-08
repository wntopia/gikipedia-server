package io.github.wntopia.gikipedia.server.domain.history.repository

import io.github.wntopia.gikipedia.server.domain.history.entity.ArticleHistorySegmentJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ArticleHistorySegmentRepository : JpaRepository<ArticleHistorySegmentJpaEntity, Long> {
    /** reconstruct() fallback에서 사용 — 스냅샷 revision(fromRevision)으로 세그먼트를 조회한다. */
    fun findByArticleIdAndFromRevision(
        articleId: Long,
        fromRevision: Int,
    ): ArticleHistorySegmentJpaEntity?

    fun existsByArticleIdAndFromRevision(
        articleId: Long,
        fromRevision: Int,
    ): Boolean

    /** listRevisions() 병합용 — article의 모든 세그먼트. */
    fun findByArticleId(articleId: Long): List<ArticleHistorySegmentJpaEntity>

    /** 압축 대상 탐색용 — compressed_payload(BLOB) 로드 없이 이미 압축된 (articleId, fromRevision) 쌍만 가져온다. */
    @Query(
        "SELECT s.article.id AS articleId, s.fromRevision AS fromRevision FROM ArticleHistorySegmentJpaEntity s",
    )
    fun findAllCompactedKeys(): List<ArticleHistorySegmentKeyView>
}

interface ArticleHistorySegmentKeyView {
    fun getArticleId(): Long

    fun getFromRevision(): Int
}
