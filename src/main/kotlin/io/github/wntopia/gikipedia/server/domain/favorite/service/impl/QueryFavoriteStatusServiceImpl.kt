package io.github.wntopia.gikipedia.server.domain.favorite.service.impl

import io.github.wntopia.gikipedia.server.domain.favorite.dto.response.FavoriteStatusResDto
import io.github.wntopia.gikipedia.server.domain.favorite.repository.ArticleFavoriteRepository
import io.github.wntopia.gikipedia.server.domain.favorite.service.QueryFavoriteStatusService
import io.github.wntopia.gikipedia.server.global.security.session.AuthenticationReader
import jakarta.servlet.http.HttpSession
import org.springframework.stereotype.Service

@Service
class QueryFavoriteStatusServiceImpl(
    private val authenticationReader: AuthenticationReader,
    private val articleFavoriteRepository: ArticleFavoriteRepository,
) : QueryFavoriteStatusService {
    /** 문서 화면은 비로그인도 열 수 있으므로, 로그인하지 않았으면 401이 아니라 favorited=false로 답한다. */
    override fun execute(
        articleId: Long,
        session: HttpSession,
    ): FavoriteStatusResDto {
        val userId = authenticationReader.getCurrentUser(session)?.id ?: return FavoriteStatusResDto(false)
        return FavoriteStatusResDto(articleFavoriteRepository.existsByUserIdAndArticleId(userId, articleId))
    }
}
