package io.github.wntopia.gikipedia.server.domain.report.repository

import io.github.wntopia.gikipedia.server.domain.report.entity.ArticleReportJpaEntity
import io.github.wntopia.gikipedia.server.domain.report.entity.ReportStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface ArticleReportRepository : JpaRepository<ArticleReportJpaEntity, Long> {
    /** 같은 사용자가 같은 문서에 아직 처리되지 않은 신고를 이미 냈는지. */
    fun existsByReporterUserIdAndArticleIdAndStatus(
        reporterUserId: Long,
        articleId: Long,
        status: ReportStatus,
    ): Boolean

    /** 신고 남용 방지용 — [since] 이후 이 사용자가 낸 신고 수. */
    fun countByReporterUserIdAndCreatedAtGreaterThanEqual(
        reporterUserId: Long,
        since: Instant,
    ): Long

    /** 관리자용 신고 목록(최신 순). 문서 제목을 함께 가져온다. [status]가 null이면 전체. */
    @Query(
        value =
            "SELECT r.id AS id, r.articleId AS articleId, a.title AS articleTitle, r.revision AS revision, " +
                "r.reason AS reason, r.reporterUserId AS reporterUserId, r.status AS status, " +
                "r.createdAt AS createdAt " +
                "FROM ArticleReportJpaEntity r JOIN ArticleJpaEntity a ON a.id = r.articleId " +
                "WHERE (:status IS NULL OR r.status = :status) ORDER BY r.createdAt DESC, r.id DESC",
        countQuery =
            "SELECT COUNT(r) FROM ArticleReportJpaEntity r " +
                "WHERE (:status IS NULL OR r.status = :status)",
    )
    fun findReports(
        @Param("status") status: ReportStatus?,
        pageable: Pageable,
    ): Page<ReportView>
}

interface ReportView {
    val id: Long
    val articleId: Long
    val articleTitle: String
    val revision: Int?
    val reason: String
    val reporterUserId: Long
    val status: ReportStatus
    val createdAt: Instant
}
