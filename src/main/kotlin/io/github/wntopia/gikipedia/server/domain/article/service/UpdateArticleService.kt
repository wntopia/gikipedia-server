package io.github.wntopia.gikipedia.server.domain.article.service

import io.github.wntopia.gikipedia.server.domain.article.dto.request.UpdateArticleReqDto
import io.github.wntopia.gikipedia.server.domain.article.dto.response.ArticleResDto

interface UpdateArticleService {
    fun execute(
        articleId: Long,
        reqDto: UpdateArticleReqDto,
    ): ArticleResDto
}
