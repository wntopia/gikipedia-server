package io.github.wntopia.gikipedia.server.domain.favorite.service

import io.github.wntopia.gikipedia.server.domain.favorite.dto.response.FavoriteStatusResDto
import jakarta.servlet.http.HttpSession

interface QueryFavoriteStatusService {
    fun execute(
        articleId: Long,
        session: HttpSession,
    ): FavoriteStatusResDto
}
