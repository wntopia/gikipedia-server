package io.github.wntopia.gikipedia.server.domain.article.dto.response

import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleJpaEntity
import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleMongoEntity
import java.time.Instant

data class ArticleResDto(
    val id: Long,
    val title: String,
    val content: String,
    val imageUrl: String?,
    val createdAt: Instant?,
    val updatedAt: Instant?,
) {
    companion object {
        fun from(entity: ArticleJpaEntity): ArticleResDto =
            ArticleResDto(
                id = requireNotNull(entity.id),
                title = entity.title,
                content = entity.content,
                imageUrl = entity.imageUrl,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
            )

        fun from(entity: ArticleMongoEntity): ArticleResDto =
            ArticleResDto(
                id = entity.documentId,
                title = entity.title,
                content = entity.content,
                imageUrl = entity.imageUrl,
                createdAt = entity.articleCreatedAt,
                updatedAt = entity.articleUpdatedAt,
            )
    }
}
