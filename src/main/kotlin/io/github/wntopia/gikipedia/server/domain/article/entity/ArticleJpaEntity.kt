package io.github.wntopia.gikipedia.server.domain.article.entity

import io.github.wntopia.gikipedia.server.global.entity.BaseJpaEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "articles")
class ArticleJpaEntity(
    title: String,
    content: String,
    imageUrl: String? = null,
    @field:ManyToOne(fetch = FetchType.LAZY)
    @field:JoinColumn(name = "parent_id")
    val parent: ArticleJpaEntity? = null,
) : BaseJpaEntity() {
    @field:Column(name = "title", nullable = false, length = 255)
    var title: String = title
        protected set

    @field:Column(name = "content", nullable = false, columnDefinition = "TEXT")
    var content: String = content
        protected set

    @field:Column(name = "image_url", length = 2048)
    var imageUrl: String? = imageUrl
        protected set

    /** title은 생성 시점에만 정해지고 이후 수정 불가능하다 — 여기서 받지 않는다. */
    fun update(
        content: String,
        imageUrl: String?,
    ) {
        this.content = content
        this.imageUrl = imageUrl
    }
}
