package io.github.wntopia.gikipedia.server.domain.user.entity

import io.github.wntopia.gikipedia.server.global.entity.BaseJpaEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * 한 번이라도 로그인한 사용자. 가입일은 첫 로그인 시점에 생성되는 이 행의 `createdAt`이다.
 *
 * 외부 계정 id([datagsmUserId])를 기준으로 식별한다. 편집자 라벨("학번 이름")은 학년이 바뀌면 달라지므로
 * 사용자 식별에 쓰지 않는다.
 */
@Entity
@Table(
    name = "users",
    uniqueConstraints = [UniqueConstraint(name = "uk_users_datagsm_user_id", columnNames = ["datagsm_user_id"])],
)
class UserJpaEntity(
    @field:Column(name = "datagsm_user_id", nullable = false, updatable = false) val datagsmUserId: Long,
    name: String?,
    role: String?,
) : BaseJpaEntity() {
    @field:Column(name = "name", length = 255)
    var name: String? = name
        protected set

    @field:Column(name = "role", length = 50)
    var role: String? = role
        protected set

    /** 로그인할 때마다 최신 계정 정보로 맞춘다. */
    fun refresh(
        name: String?,
        role: String?,
    ) {
        this.name = name
        this.role = role
    }
}
