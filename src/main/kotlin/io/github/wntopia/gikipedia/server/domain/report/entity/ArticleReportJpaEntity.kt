package io.github.wntopia.gikipedia.server.domain.report.entity

import io.github.wntopia.gikipedia.server.global.entity.BaseJpaEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/** 문서(또는 문서의 특정 리비전)에 대한 신고. */
@Entity
@Table(
    name = "article_reports",
    uniqueConstraints = [UniqueConstraint(name = "uk_reports_open_key", columnNames = ["open_key"])],
    indexes = [
        Index(name = "idx_reports_status_created", columnList = "status, created_at"),
        Index(name = "idx_reports_reporter_article", columnList = "reporter_user_id, article_id"),
    ],
)
class ArticleReportJpaEntity(
    /** 신고한 사용자의 DataGSM 계정 id. */
    @field:Column(name = "reporter_user_id", nullable = false, updatable = false) val reporterUserId: Long,
    @field:Column(name = "article_id", nullable = false, updatable = false) val articleId: Long,
    /** 신고 대상 리비전. 문서 전체를 신고하면 null. */
    @field:Column(name = "revision", updatable = false) val revision: Int?,
    @field:Column(name = "reason", nullable = false, updatable = false, length = 1000) val reason: String,
) : BaseJpaEntity() {
    @field:Enumerated(EnumType.STRING)
    @field:Column(name = "status", nullable = false, length = 20)
    var status: ReportStatus = ReportStatus.OPEN
        protected set

    /**
     * 처리되지 않은 신고에만 값이 있는 중복 방지 키("신고자:문서"). 유니크 제약이 걸려 있어 같은 사용자가 같은 문서에
     * 처리되지 않은 신고를 동시에 두 건 만들 수 없다. 처리되면 null로 비워(NULL은 유니크 판정에서 제외) 다시 신고할 수 있다.
     */
    @field:Column(name = "open_key", length = 64)
    var openKey: String? = "$reporterUserId:$articleId"
        protected set

    fun resolve() {
        status = ReportStatus.RESOLVED
        openKey = null
    }
}
