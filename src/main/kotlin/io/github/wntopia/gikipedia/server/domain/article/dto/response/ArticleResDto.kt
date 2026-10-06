package io.github.wntopia.gikipedia.server.domain.article.dto.response

import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleJpaEntity
import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleMongoEntity
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

data class ArticleResDto(
    @field:Schema(description = "문서 ID", example = "1")
    val id: Long,
    @field:Schema(description = "문서 제목")
    val title: String,
    @field:Schema(description = "문서 본문")
    val content: String,
    @field:Schema(description = "대표 이미지 URL (없으면 null)")
    val imageUrl: String?,
    @field:Schema(description = "생성 시각 (ISO-8601, UTC)")
    val createdAt: Instant?,
    @field:Schema(description = "마지막 수정 시각 (ISO-8601, UTC)")
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
