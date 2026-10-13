package io.github.wntopia.gikipedia.server.domain.favorite.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class FavoriteStatusResDto(
    @field:Schema(description = "내 즐겨찾기에 들어 있는지 여부 (비로그인이면 항상 false)")
    val favorited: Boolean,
)
