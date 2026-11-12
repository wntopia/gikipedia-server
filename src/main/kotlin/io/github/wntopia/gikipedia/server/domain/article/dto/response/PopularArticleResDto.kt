package io.github.wntopia.gikipedia.server.domain.article.dto.response

import io.github.wntopia.gikipedia.server.domain.article.repository.ArticlePopularView
import io.swagger.v3.oas.annotations.media.Schema

data class PopularArticleResDto(
    @field:Schema(description = "문서 ID", example = "1")
    val id: Long,
    @field:Schema(description = "문서 제목")
    val title: String,
    @field:Schema(description = "누적 조회수", example = "128")
    val viewCount: Long,
) {
    companion object {
        fun from(view: ArticlePopularView): PopularArticleResDto =
            PopularArticleResDto(view.id, view.title, view.viewCount)
    }
}
