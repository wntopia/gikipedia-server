package io.github.wntopia.gikipedia.server.domain.favorite.dto.response

import io.github.wntopia.gikipedia.server.domain.favorite.repository.FavoriteView
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

data class FavoriteResDto(
    @field:Schema(description = "문서 ID", example = "1")
    val articleId: Long,
    @field:Schema(description = "문서 제목")
    val title: String,
    @field:Schema(description = "즐겨찾기에 추가한 시각 (ISO-8601, UTC)")
    val favoritedAt: Instant,
) {
    companion object {
        fun from(view: FavoriteView): FavoriteResDto = FavoriteResDto(view.articleId, view.title, view.favoritedAt)
    }
}
