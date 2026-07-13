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
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    val parent: ArticleJpaEntity? = null,
) : BaseJpaEntity() {
    @Column(name = "title", nullable = false, length = 255)
    var title: String = title
        protected set

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    var content: String = content
        protected set

    @Column(name = "image_url", length = 2048)
    var imageUrl: String? = imageUrl
        protected set

    fun update(
        title: String,
        content: String,
        imageUrl: String?,
    ) {
        this.title = title
        this.content = content
        this.imageUrl = imageUrl
    }
}
