package io.github.wntopia.gikipedia.server.domain.article.service.impl

import io.github.wntopia.gikipedia.server.domain.article.dto.request.UpdateArticleReqDto
import io.github.wntopia.gikipedia.server.domain.article.dto.response.ArticleResDto
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.domain.article.service.ArticleValidator
import io.github.wntopia.gikipedia.server.domain.article.service.UpdateArticleService
import io.github.wntopia.gikipedia.server.global.storage.R2Uploader
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import team.themoment.sdk.exception.ExpectedException

@Service
class UpdateArticleServiceImpl(
    private val articleRepository: ArticleRepository,
    private val r2Uploader: R2Uploader,
    private val articleValidator: ArticleValidator,
) : UpdateArticleService {
    @Transactional
    override fun execute(
        articleId: Long,
        reqDto: UpdateArticleReqDto,
    ): ArticleResDto {
        val article =
            articleRepository
                .findById(articleId)
                .orElseThrow { ExpectedException("존재하지 않는 게시글입니다.", HttpStatus.NOT_FOUND) }
        articleValidator.validate(reqDto.title, reqDto.content)

        val imageUrl =
            reqDto.image?.takeIf { !it.isEmpty }?.let { r2Uploader.upload(it, IMAGE_KEY_PREFIX) }
                ?: article.imageUrl

        article.update(reqDto.title, reqDto.content, imageUrl)
        return ArticleResDto.from(article)
    }

    companion object {
        private const val IMAGE_KEY_PREFIX = "articles"
    }
}
