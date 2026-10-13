package io.github.wntopia.gikipedia.server.domain.article.service

import io.github.wntopia.gikipedia.server.domain.article.dto.response.ArticleEditorsResDto

interface QueryArticleEditorsService {
    fun execute(articleId: Long): ArticleEditorsResDto
}
