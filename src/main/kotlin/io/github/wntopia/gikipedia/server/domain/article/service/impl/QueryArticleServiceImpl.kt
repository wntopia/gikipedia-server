package io.github.wntopia.gikipedia.server.domain.article.service.impl

import io.github.wntopia.gikipedia.server.domain.article.dto.response.ArticleResDto
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.domain.article.service.QueryArticleService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import team.themoment.sdk.exception.ExpectedException

@Service
class QueryArticleServiceImpl(
    private val articleRepository: ArticleRepository,
) : QueryArticleService {
    @Transactional(readOnly = true)
    override fun execute(articleId: Long): ArticleResDto {
        val article =
            articleRepository
                .findById(articleId)
                .orElseThrow { ExpectedException("존재하지 않는 게시글입니다.", HttpStatus.NOT_FOUND) }
        return ArticleResDto.from(article)
    }
}
