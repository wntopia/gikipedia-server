package io.github.wntopia.gikipedia.server.domain.article.service

import io.github.wntopia.gikipedia.server.domain.article.dto.request.CreateArticleReqDto
import io.github.wntopia.gikipedia.server.domain.article.dto.response.ArticleResDto

interface CreateArticleService {
    fun execute(reqDto: CreateArticleReqDto): ArticleResDto
}
