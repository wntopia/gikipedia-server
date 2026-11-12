package io.github.wntopia.gikipedia.server.domain.category.dto.response

import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRecentView
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

data class CategoryArticleResDto(
    @field:Schema(description = "문서 ID", example = "1")
    val id: Long,
    @field:Schema(description = "문서 제목")
    val title: String,
    @field:Schema(description = "마지막 수정 시각 (ISO-8601, UTC)")
    val updatedAt: Instant,
) {
    companion object {
        fun from(view: ArticleRecentView): CategoryArticleResDto =
            CategoryArticleResDto(view.id, view.title, view.updatedAt)
    }
}
