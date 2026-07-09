package io.github.wntopia.gikipedia.server.global.entity

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import java.time.Instant

abstract class BaseMongoEntity {
    @Id val id: String? = null

    @CreatedDate
    var createdAt: Instant? = null
        protected set

    @LastModifiedDate
    var updatedAt: Instant? = null
        protected set
}
