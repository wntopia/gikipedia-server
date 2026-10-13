package io.github.wntopia.gikipedia.server.domain.favorite.service

import io.github.wntopia.gikipedia.server.domain.favorite.dto.response.FavoriteResDto
import io.github.wntopia.gikipedia.server.global.dto.PageResDto
import jakarta.servlet.http.HttpSession

interface QueryMyFavoritesService {
    fun execute(
        session: HttpSession,
        page: Int,
        size: Int,
    ): PageResDto<FavoriteResDto>
}
