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

    /** [since] 이후의 전체 기여(문서 생성·수정) 수. */
    fun countByCreatedAtGreaterThanEqual(since: Instant): Long

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

    /** 주어진 문서들의 [since] 이후 수정 횟수와 수정한 사람 수. 오늘 수정이 없는 문서는 결과에 없다. */
    @Query(
        "SELECT c.articleId AS articleId, COUNT(c) AS editCount, COUNT(DISTINCT c.userId) AS editorCount " +
            "FROM ContributionJpaEntity c " +
            "WHERE c.articleId IN :articleIds AND c.type = " +
            "io.github.wntopia.gikipedia.server.domain.user.entity.ContributionType.EDIT AND c.createdAt >= :since " +
            "GROUP BY c.articleId",
    )
    fun findEditStatsSince(
        @Param("articleIds") articleIds: Collection<Long>,
        @Param("since") since: Instant,
    ): List<ArticleEditStatsView>

    /** 주어진 문서들 각각의 가장 최근 기여 한 건. */
    @Query(
        "SELECT c FROM ContributionJpaEntity c WHERE c.id IN " +
            "(SELECT MAX(c2.id) FROM ContributionJpaEntity c2 WHERE c2.articleId IN :articleIds GROUP BY c2.articleId)",
    )
    fun findLatestByArticleIds(
        @Param("articleIds") articleIds: Collection<Long>,
    ): List<ContributionJpaEntity>
}

interface ArticleEditStatsView {
    val articleId: Long
    val editCount: Long
    val editorCount: Long
}

interface ContributionView {
    val articleId: Long
    val title: String
    val type: ContributionType
    val contributedAt: Instant
}
