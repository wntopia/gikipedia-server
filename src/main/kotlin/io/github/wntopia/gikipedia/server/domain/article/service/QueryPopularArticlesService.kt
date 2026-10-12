package io.github.wntopia.gikipedia.server.domain.article.service

import io.github.wntopia.gikipedia.server.domain.article.dto.response.PopularArticleResDto

interface QueryPopularArticlesService {
    fun execute(size: Int): List<PopularArticleResDto>
}
