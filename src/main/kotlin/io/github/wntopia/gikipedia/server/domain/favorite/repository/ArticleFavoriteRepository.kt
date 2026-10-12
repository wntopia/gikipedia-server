package io.github.wntopia.gikipedia.server.domain.favorite.repository

import io.github.wntopia.gikipedia.server.domain.favorite.entity.ArticleFavoriteJpaEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface ArticleFavoriteRepository : JpaRepository<ArticleFavoriteJpaEntity, Long> {
    fun existsByUserIdAndArticleId(
        userId: Long,
        articleId: Long,
    ): Boolean

    @Modifying
    @Query("DELETE FROM ArticleFavoriteJpaEntity f WHERE f.userId = :userId AND f.articleId = :articleId")
    fun deleteByUserIdAndArticleId(
        @Param("userId") userId: Long,
        @Param("articleId") articleId: Long,
    ): Int

    /** 내 즐겨찾기 목록(최근에 추가한 순). 문서 제목을 함께 가져온다. */
    @Query(
        value =
            "SELECT f.articleId AS articleId, a.title AS title, f.createdAt AS favoritedAt " +
                "FROM ArticleFavoriteJpaEntity f JOIN ArticleJpaEntity a ON a.id = f.articleId " +
                "WHERE f.userId = :userId ORDER BY f.createdAt DESC, f.id DESC",
        countQuery = "SELECT COUNT(f) FROM ArticleFavoriteJpaEntity f WHERE f.userId = :userId",
    )
    fun findByUserId(
        @Param("userId") userId: Long,
        pageable: Pageable,
    ): Page<FavoriteView>
}

interface FavoriteView {
    val articleId: Long
    val title: String
    val favoritedAt: Instant
}
