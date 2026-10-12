package io.github.wntopia.gikipedia.server.domain.user.repository

import io.github.wntopia.gikipedia.server.domain.user.entity.ContributionJpaEntity
import io.github.wntopia.gikipedia.server.domain.user.entity.ContributionType
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface ContributionRepository : JpaRepository<ContributionJpaEntity, Long> {
    fun countByUserIdAndType(
        userId: Long,
        type: ContributionType,
    ): Long

    fun countByUserIdAndTypeAndCreatedAtGreaterThanEqual(
        userId: Long,
        type: ContributionType,
        since: Instant,
    ): Long

    /** 최근 기여 목록(최신 순). 문서 제목을 함께 가져온다. */
    @Query(
        "SELECT c.articleId AS articleId, a.title AS title, c.type AS type, c.createdAt AS contributedAt " +
            "FROM ContributionJpaEntity c JOIN ArticleJpaEntity a ON a.id = c.articleId " +
            "WHERE c.userId = :userId ORDER BY c.createdAt DESC, c.id DESC",
    )
    fun findRecentByUserId(
        @Param("userId") userId: Long,
        pageable: Pageable,
    ): List<ContributionView>
}

interface ContributionView {
    val articleId: Long
    val title: String
    val type: ContributionType
    val contributedAt: Instant
}
