package io.github.wntopia.gikipedia.server.domain.article.service

import io.github.wntopia.gikipedia.server.domain.article.dto.response.ArticleResDto

interface QueryArticleService {
    fun execute(articleId: Long): ArticleResDto
}
