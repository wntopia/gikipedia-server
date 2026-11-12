package io.github.wntopia.gikipedia.server.domain.article.service

import io.github.wntopia.gikipedia.server.domain.article.dto.request.CreateArticleReqDto
import io.github.wntopia.gikipedia.server.domain.article.dto.response.ArticleResDto
import jakarta.servlet.http.HttpSession

interface CreateArticleService {
    fun execute(
        reqDto: CreateArticleReqDto,
        session: HttpSession,
    ): ArticleResDto
}
