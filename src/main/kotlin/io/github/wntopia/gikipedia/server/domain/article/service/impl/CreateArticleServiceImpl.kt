package io.github.wntopia.gikipedia.server.domain.article.service.impl

import io.github.wntopia.gikipedia.server.domain.article.dto.request.CreateArticleReqDto
import io.github.wntopia.gikipedia.server.domain.article.dto.response.ArticleResDto
import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleJpaEntity
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.domain.article.service.ArticleCacheService
import io.github.wntopia.gikipedia.server.domain.article.service.CreateArticleService
import io.github.wntopia.gikipedia.server.domain.history.event.ArticleUpdatedEvent
import io.github.wntopia.gikipedia.server.global.storage.R2Uploader
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CreateArticleServiceImpl(
    private val articleRepository: ArticleRepository,
    private val r2Uploader: R2Uploader,
    private val eventPublisher: ApplicationEventPublisher,
    private val articleCacheService: ArticleCacheService,
) : CreateArticleService {
    @Transactional
    override fun execute(reqDto: CreateArticleReqDto): ArticleResDto {
        val imageUrl = reqDto.image?.takeIf { !it.isEmpty }?.let { r2Uploader.upload(it, IMAGE_KEY_PREFIX) }
        val saved = articleRepository.save(ArticleJpaEntity(reqDto.title, reqDto.content, imageUrl))

        // article_histories의 리비전 체계("리비전 1 = 생성 시점 그대로")와 맞춰, Mongo 뷰도 생성 시점에
        // 리비전 1로 채워둔다. 이걸 빼먹으면 한 번도 수정 안 된 문서는 Mongo에 영영 없는 상태로 남는다.
        eventPublisher.publishEvent(ArticleUpdatedEvent(requireNotNull(saved.id), BASELINE_REVISION))

        val response = ArticleResDto.from(saved)
        articleCacheService.putAfterCommit(response)
        return response
    }

    companion object {
        private const val IMAGE_KEY_PREFIX = "articles"
        private const val BASELINE_REVISION = 1
    }
}
