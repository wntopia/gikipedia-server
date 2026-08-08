package io.github.wntopia.gikipedia.server.domain.history.repository

import io.github.wntopia.gikipedia.server.domain.history.entity.ArticleSnapshotJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ArticleSnapshotRepository : JpaRepository<ArticleSnapshotJpaEntity, Long> {
    /** 대상 리비전 이하에서 가장 가까운(리비전이 가장 큰) 스냅샷. 재구성의 시작점이 된다. */
    fun findTopByArticleIdAndRevisionLessThanEqualOrderByRevisionDesc(
        articleId: Long,
        revision: Int,
    ): ArticleSnapshotJpaEntity?

    /** 히스토리 압축 배치가 압축 대상 스냅샷 row를 찾아 압축 페이로드를 채워 넣을 때 사용한다. */
    fun findByArticleIdAndRevision(
        articleId: Long,
        revision: Int,
    ): ArticleSnapshotJpaEntity?

    /** listRevisions() 병합용 — 압축 페이로드가 실제로 채워진 스냅샷만 가져온다(대부분은 null이라 대상이 적다). */
    fun findByArticleIdAndCompressedInteriorPayloadIsNotNull(articleId: Long): List<ArticleSnapshotJpaEntity>

    /**
     * 히스토리 압축 배치의 대상 탐색용 — content/compressed_interior_payload(TEXT/BLOB) 로드 없이
     * article별 스냅샷 revision과 압축 여부만 전부 가져온다. 압축 여부는 이 스냅샷 자신의
     * compressedInteriorPayload가 채워져 있는지로 판단한다(다음 스냅샷까지의 interior가 이미 압축됐다는 뜻).
     */
    @Query(
        "SELECT s.article.id AS articleId, s.revision AS revision, " +
            "CASE WHEN s.compressedInteriorPayload IS NOT NULL THEN true ELSE false END AS compacted " +
            "FROM ArticleSnapshotJpaEntity s ORDER BY s.article.id ASC, s.revision ASC",
    )
    fun findAllRevisionsOrderByArticleAscRevisionAsc(): List<ArticleSnapshotRevisionView>
}

interface ArticleSnapshotRevisionView {
    fun getArticleId(): Long

    fun getRevision(): Int

    fun getCompacted(): Boolean
}
