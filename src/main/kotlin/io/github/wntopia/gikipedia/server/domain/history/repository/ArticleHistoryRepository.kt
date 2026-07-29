package io.github.wntopia.gikipedia.server.domain.history.repository

import io.github.wntopia.gikipedia.server.domain.history.entity.ArticleHistoryJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ArticleHistoryRepository : JpaRepository<ArticleHistoryJpaEntity, Long> {
    /** 스냅샷 리비전(from) 다음부터 대상 리비전(to)까지의 diff 조각을 순차 적용 순서로 조회한다. */
    fun findByArticleIdAndRevisionBetweenOrderByRevisionAsc(
        articleId: Long,
        from: Int,
        to: Int,
    ): List<ArticleHistoryJpaEntity>

    /** 문서의 현재 최신 리비전 기록. 다음 리비전 번호 채번 및 최신 여부 판별에 사용한다. */
    fun findTopByArticleIdOrderByRevisionDesc(articleId: Long): ArticleHistoryJpaEntity?

    /** 히스토리 목록(버전 히스토리 페이지)용. 최신 리비전이 먼저 온다. */
    fun findByArticleIdOrderByRevisionDesc(articleId: Long): List<ArticleHistoryJpaEntity>

    /** 일일 정합성 검사 배치용 — article별 최신(=MAX) 리비전을 한 번에 조회한다. */
    @Query(
        "SELECT h.article.id AS articleId, MAX(h.revision) AS revision FROM ArticleHistoryJpaEntity h GROUP BY h.article.id",
    )
    fun findLatestRevisionPerArticle(): List<ArticleLatestRevisionView>
}

interface ArticleLatestRevisionView {
    fun getArticleId(): Long

    fun getRevision(): Int
}
