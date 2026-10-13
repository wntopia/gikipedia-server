package io.github.wntopia.gikipedia.server.domain.article.service.impl

import io.github.wntopia.gikipedia.server.domain.article.dto.response.PopularArticleResDto
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.domain.article.service.QueryPopularArticlesService
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class QueryPopularArticlesServiceImpl(
    private val articleRepository: ArticleRepository,
) : QueryPopularArticlesService {
    @Transactional(readOnly = true)
    override fun execute(size: Int): List<PopularArticleResDto> =
        articleRepository.findPopular(PageRequest.of(0, size)).map(PopularArticleResDto::from)
}
