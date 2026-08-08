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

    /** 히스토리 압축 배치의 대상 탐색용 — content(TEXT) 로드 없이 article별 스냅샷 revision만 전부 가져온다. */
    @Query(
        "SELECT s.article.id AS articleId, s.revision AS revision FROM ArticleSnapshotJpaEntity s " +
            "ORDER BY s.article.id ASC, s.revision ASC",
    )
    fun findAllRevisionsOrderByArticleAscRevisionAsc(): List<ArticleSnapshotRevisionView>
}

interface ArticleSnapshotRevisionView {
    fun getArticleId(): Long

    fun getRevision(): Int
}
