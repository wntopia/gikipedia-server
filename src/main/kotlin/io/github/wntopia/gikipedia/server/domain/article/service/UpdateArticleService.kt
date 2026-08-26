package io.github.wntopia.gikipedia.server.domain.article.service

import io.github.wntopia.gikipedia.server.domain.article.dto.request.UpdateArticleReqDto
import io.github.wntopia.gikipedia.server.domain.article.dto.response.ArticleResDto
import jakarta.servlet.http.HttpSession

interface UpdateArticleService {
    fun execute(
        articleId: Long,
        reqDto: UpdateArticleReqDto,
        session: HttpSession,
    ): ArticleResDto
}
