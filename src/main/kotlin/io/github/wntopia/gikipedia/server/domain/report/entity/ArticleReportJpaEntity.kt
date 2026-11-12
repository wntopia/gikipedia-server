package io.github.wntopia.gikipedia.server.domain.report.entity

import io.github.wntopia.gikipedia.server.global.entity.BaseJpaEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * 문서의 특정 리비전에 대한 신고. 같은 사용자는 같은 리비전을 한 번만 신고할 수 있다.
 *
 * 리비전은 FK가 아니라 (문서 id, 리비전 번호)로 가리킨다 — 히스토리 압축이 중간 리비전 row를 지워도 번호는 그대로
 * 유효하기 때문이다.
 */
@Entity
@Table(
    name = "article_reports",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_reports_user_article_revision",
            columnNames = ["user_id", "article_id", "revision"],
        ),
    ],
)
class ArticleReportJpaEntity(
    /** 신고한 사용자의 DataGSM 계정 id. */
    @field:Column(name = "user_id", nullable = false, updatable = false) val userId: Long,
    @field:Column(name = "article_id", nullable = false, updatable = false) val articleId: Long,
    @field:Column(name = "revision", nullable = false, updatable = false) val revision: Int,
    @field:Column(name = "reason", nullable = false, updatable = false, length = 500) val reason: String,
) : BaseJpaEntity()
