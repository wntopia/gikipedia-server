package io.github.wntopia.gikipedia.server.global.entity

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseJpaEntity {
    @field:Id
    @field:GeneratedValue(strategy = GenerationType.IDENTITY)
    @field:Column(name = "id", nullable = false, updatable = false)
    val id: Long? = null

    @field:CreatedDate
    @field:Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null
        protected set

    @field:LastModifiedDate
    @field:Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null
        protected set
}
