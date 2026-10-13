package io.github.wntopia.gikipedia.server.domain.user.repository

import io.github.wntopia.gikipedia.server.domain.user.entity.UserJpaEntity
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<UserJpaEntity, Long> {
    fun findByDatagsmUserId(datagsmUserId: Long): UserJpaEntity?

    fun findByDatagsmUserIdIn(datagsmUserIds: Collection<Long>): List<UserJpaEntity>
}
