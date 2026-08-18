package io.github.wntopia.gikipedia.server.domain.article.service

import io.github.wntopia.gikipedia.server.domain.article.dto.request.UpdateArticleImageReqDto
import io.github.wntopia.gikipedia.server.domain.article.dto.response.ArticleResDto
import jakarta.servlet.http.HttpSession

interface UpdateArticleImageService {
    fun execute(
        articleId: Long,
        reqDto: UpdateArticleImageReqDto,
        session: HttpSession,
    ): ArticleResDto
}
