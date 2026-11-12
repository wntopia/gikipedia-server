package io.github.wntopia.gikipedia.server.domain.report.repository

import io.github.wntopia.gikipedia.server.domain.report.entity.ArticleReportJpaEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.Instant

interface ArticleReportRepository : JpaRepository<ArticleReportJpaEntity, Long> {
    fun existsByUserIdAndArticleIdAndRevision(
        userId: Long,
        articleId: Long,
        revision: Int,
    ): Boolean

    /** 관리자용 신고 목록(최근 접수 순). 문서 제목을 함께 가져온다. */
    @Query(
        value =
            "SELECT r.id AS id, r.articleId AS articleId, a.title AS title, r.revision AS revision, " +
                "r.reason AS reason, r.userId AS reporterUserId, r.createdAt AS reportedAt " +
                "FROM ArticleReportJpaEntity r JOIN ArticleJpaEntity a ON a.id = r.articleId " +
                "ORDER BY r.createdAt DESC, r.id DESC",
        countQuery = "SELECT COUNT(r) FROM ArticleReportJpaEntity r JOIN ArticleJpaEntity a ON a.id = r.articleId",
    )
    fun findAllWithTitle(pageable: Pageable): Page<ReportView>
}

interface ReportView {
    val id: Long
    val articleId: Long
    val title: String
    val revision: Int
    val reason: String
    val reporterUserId: Long
    val reportedAt: Instant
}
