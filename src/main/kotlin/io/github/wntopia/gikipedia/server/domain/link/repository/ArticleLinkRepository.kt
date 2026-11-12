package io.github.wntopia.gikipedia.server.domain.link.repository

import io.github.wntopia.gikipedia.server.domain.link.entity.ArticleLinkJpaEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ArticleLinkRepository : JpaRepository<ArticleLinkJpaEntity, Long> {
    @Query("SELECT l.targetKey FROM ArticleLinkJpaEntity l WHERE l.sourceArticleId = :sourceArticleId")
    fun findTargetKeysBySourceArticleId(
        @Param("sourceArticleId") sourceArticleId: Long,
    ): List<String>

    @Modifying
    @Query(
        "DELETE FROM ArticleLinkJpaEntity l WHERE l.sourceArticleId = :sourceArticleId AND l.targetKey IN :targetKeys",
    )
    fun deleteBySourceArticleIdAndTargetKeyIn(
        @Param("sourceArticleId") sourceArticleId: Long,
        @Param("targetKeys") targetKeys: Collection<String>,
    ): Int

    /** [targetKey] 제목을 링크한 문서들. 제목 순으로 정렬한다. */
    @Query(
        value =
            "SELECT a.id AS id, a.title AS title FROM ArticleLinkJpaEntity l " +
                "JOIN ArticleJpaEntity a ON a.id = l.sourceArticleId " +
                "WHERE l.targetKey = :targetKey ORDER BY a.title, a.id",
        countQuery = "SELECT COUNT(l) FROM ArticleLinkJpaEntity l WHERE l.targetKey = :targetKey",
    )
    fun findBacklinks(
        @Param("targetKey") targetKey: String,
        pageable: Pageable,
    ): Page<BacklinkView>
}

interface BacklinkView {
    val id: Long
    val title: String
}
