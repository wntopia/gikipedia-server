package io.github.wntopia.gikipedia.server.domain.history.repository

import io.github.wntopia.gikipedia.server.domain.history.entity.ArticleHistoryJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

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

    /** 일일 정합성 검사 배치용 — 주어진 article 청크에 한해 article별 최신(=MAX) 리비전을 한 번에 조회한다. */
    @Query(
        "SELECT h.article.id AS articleId, MAX(h.revision) AS revision FROM ArticleHistoryJpaEntity h " +
            "WHERE h.article.id IN :articleIds GROUP BY h.article.id",
    )
    fun findLatestRevisionPerArticleIn(
        @Param("articleIds") articleIds: Collection<Long>,
    ): List<ArticleLatestRevisionView>

    /** reconstruct() fallback에서 스냅샷 경계 리비전 1건의 editor/createdAt을 직접 조회한다. */
    fun findByArticleIdAndRevision(
        articleId: Long,
        revision: Int,
    ): ArticleHistoryJpaEntity?

    /**
     * 압축 완료 후 원본 interior row를 한 번의 DELETE 문으로 제거한다.
     *
     * 파생 delete 메서드(deleteBy...)는 대상을 먼저 SELECT한 뒤 개별 DELETE를 날리지만, 여기서는
     * 라이프사이클 콜백이 필요 없는 순수 append-only row라 JPQL bulk delete로 한 번에 처리한다.
     */
    @Modifying(clearAutomatically = true)
    @Query(
        "DELETE FROM ArticleHistoryJpaEntity h WHERE h.article.id = :articleId AND h.revision BETWEEN :from AND :to",
    )
    fun deleteInteriorRevisions(
        @Param("articleId") articleId: Long,
        @Param("from") from: Int,
        @Param("to") to: Int,
    ): Int
}

interface ArticleLatestRevisionView {
    fun getArticleId(): Long

    fun getRevision(): Int
}
