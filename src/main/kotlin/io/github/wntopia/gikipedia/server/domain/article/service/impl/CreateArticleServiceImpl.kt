package io.github.wntopia.gikipedia.server.domain.article.service.impl

import io.github.wntopia.gikipedia.server.domain.article.dto.request.CreateArticleReqDto
import io.github.wntopia.gikipedia.server.domain.article.dto.response.ArticleResDto
import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleJpaEntity
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.domain.article.service.ArticleValidator
import io.github.wntopia.gikipedia.server.domain.article.service.CreateArticleService
import io.github.wntopia.gikipedia.server.global.storage.R2Uploader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CreateArticleServiceImpl(
    private val articleRepository: ArticleRepository,
    private val r2Uploader: R2Uploader,
    private val articleValidator: ArticleValidator,
) : CreateArticleService {
    @Transactional
    override fun execute(reqDto: CreateArticleReqDto): ArticleResDto {
        articleValidator.validate(reqDto.title, reqDto.content)
        val imageUrl = reqDto.image?.takeIf { !it.isEmpty }?.let { r2Uploader.upload(it, IMAGE_KEY_PREFIX) }
        val saved = articleRepository.save(ArticleJpaEntity(reqDto.title, reqDto.content, imageUrl))
        return ArticleResDto.from(saved)
    }

    companion object {
        private const val IMAGE_KEY_PREFIX = "articles"
    }
}
