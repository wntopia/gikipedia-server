package io.github.wntopia.gikipedia.server.domain.favorite.service.impl

import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.domain.favorite.entity.ArticleFavoriteJpaEntity
import io.github.wntopia.gikipedia.server.domain.favorite.repository.ArticleFavoriteRepository
import io.github.wntopia.gikipedia.server.domain.favorite.service.AddFavoriteService
import io.github.wntopia.gikipedia.server.global.security.session.AuthenticationReader
import jakarta.servlet.http.HttpSession
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import team.themoment.sdk.exception.ExpectedException

@Service
class AddFavoriteServiceImpl(
    private val authenticationReader: AuthenticationReader,
    private val articleRepository: ArticleRepository,
    private val articleFavoriteRepository: ArticleFavoriteRepository,
) : AddFavoriteService {
    /** 이미 즐겨찾기한 문서를 다시 추가해도 한 번만 들어가며 오류 없이 끝난다(멱등). */
    @Transactional
    override fun execute(
        articleId: Long,
        session: HttpSession,
    ) {
        val userId = authenticationReader.requireUserId(session)
        if (!articleRepository.existsById(articleId)) {
            throw ExpectedException("존재하지 않는 게시글입니다.", HttpStatus.NOT_FOUND)
        }
        if (articleFavoriteRepository.existsByUserIdAndArticleId(userId, articleId)) return

        try {
            articleFavoriteRepository.saveAndFlush(ArticleFavoriteJpaEntity(userId, articleId))
        } catch (e: DataIntegrityViolationException) {
            // 같은 요청이 동시에 두 번 들어와 유니크 제약에 걸린 경우 — 이미 들어 있으므로 성공으로 본다.
        }
    }
}
