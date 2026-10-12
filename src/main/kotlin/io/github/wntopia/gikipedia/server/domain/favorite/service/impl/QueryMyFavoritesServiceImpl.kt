package io.github.wntopia.gikipedia.server.domain.favorite.service.impl

import io.github.wntopia.gikipedia.server.domain.favorite.dto.response.FavoriteResDto
import io.github.wntopia.gikipedia.server.domain.favorite.repository.ArticleFavoriteRepository
import io.github.wntopia.gikipedia.server.domain.favorite.service.QueryMyFavoritesService
import io.github.wntopia.gikipedia.server.global.dto.PageResDto
import io.github.wntopia.gikipedia.server.global.security.session.AuthenticationReader
import jakarta.servlet.http.HttpSession
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class QueryMyFavoritesServiceImpl(
    private val authenticationReader: AuthenticationReader,
    private val articleFavoriteRepository: ArticleFavoriteRepository,
) : QueryMyFavoritesService {
    @Transactional(readOnly = true)
    override fun execute(
        session: HttpSession,
        page: Int,
        size: Int,
    ): PageResDto<FavoriteResDto> {
        val userId = authenticationReader.requireUserId(session)
        val result = articleFavoriteRepository.findByUserId(userId, PageRequest.of(page, size))
        return PageResDto(result.content.map(FavoriteResDto::from), page, size, result.totalElements, result.hasNext())
    }
}
