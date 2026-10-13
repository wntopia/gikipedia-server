package io.github.wntopia.gikipedia.server.domain.favorite.service.impl

import io.github.wntopia.gikipedia.server.domain.favorite.repository.ArticleFavoriteRepository
import io.github.wntopia.gikipedia.server.domain.favorite.service.RemoveFavoriteService
import io.github.wntopia.gikipedia.server.global.security.session.AuthenticationReader
import jakarta.servlet.http.HttpSession
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RemoveFavoriteServiceImpl(
    private val authenticationReader: AuthenticationReader,
    private val articleFavoriteRepository: ArticleFavoriteRepository,
) : RemoveFavoriteService {
    /** 즐겨찾기에 없는 문서를 삭제해도 오류 없이 끝난다(멱등). */
    @Transactional
    override fun execute(
        articleId: Long,
        session: HttpSession,
    ) {
        articleFavoriteRepository.deleteByUserIdAndArticleId(authenticationReader.requireUserId(session), articleId)
    }
}
