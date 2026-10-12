package io.github.wntopia.gikipedia.server.domain.favorite.service

import jakarta.servlet.http.HttpSession

interface RemoveFavoriteService {
    fun execute(
        articleId: Long,
        session: HttpSession,
    )
}
