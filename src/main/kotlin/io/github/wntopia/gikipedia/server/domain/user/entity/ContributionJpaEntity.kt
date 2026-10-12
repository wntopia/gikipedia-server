package io.github.wntopia.gikipedia.server.domain.user.entity

import io.github.wntopia.gikipedia.server.global.entity.BaseJpaEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table

/**
 * 사용자의 기여(문서 생성 / 수정)를 append-only로 쌓는 원장.
 *
 * `article_histories`로 집계하지 않는 이유: 히스토리 압축이 오래된 리비전 행을 삭제하므로 그 행 수로는 기여
 * 수가 시간이 지날수록 줄어든다. 이 테이블은 압축의 영향을 받지 않는다.
 */
@Entity
@Table(
    name = "user_contributions",
    indexes = [Index(name = "idx_contributions_user_created", columnList = "user_id, created_at")],
)
class ContributionJpaEntity(
    /**
     * [UserJpaEntity.datagsmUserId]와 같은 값. 비로그인으로 만든 문서의 생성 기여만 null이다(수정은 로그인이 필요).
     * `ddl-auto: update`는 이미 만든 컬럼의 NOT NULL을 풀지 않으므로 컬럼은 처음부터 nullable로 만든다.
     */
    @field:Column(name = "user_id", updatable = false) val userId: Long?,
    @field:Column(name = "article_id", nullable = false, updatable = false) val articleId: Long,
    @field:Enumerated(EnumType.STRING)
    @field:Column(name = "type", nullable = false, updatable = false, length = 20)
    val type: ContributionType,
    /** 이 기여가 만든 리비전 번호. 문서 생성은 null. */
    @field:Column(name = "revision", updatable = false) val revision: Int? = null,
) : BaseJpaEntity()
