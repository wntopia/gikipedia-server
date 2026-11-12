package io.github.wntopia.gikipedia.server.domain.article.service

import io.github.wntopia.gikipedia.server.domain.article.dto.request.SearchArticleReqDto
import io.github.wntopia.gikipedia.server.domain.article.dto.response.ArticleSearchResDto

interface SearchArticleService {
    fun execute(reqDto: SearchArticleReqDto): ArticleSearchResDto
}
