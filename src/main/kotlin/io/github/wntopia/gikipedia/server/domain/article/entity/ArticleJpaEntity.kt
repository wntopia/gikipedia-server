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
    @Column(name = "title", nullable = false, length = 255) val title: String,
    @Column(name = "content", nullable = false, columnDefinition = "TEXT") val content: String,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    val parent: ArticleJpaEntity? = null,
) : BaseJpaEntity()
