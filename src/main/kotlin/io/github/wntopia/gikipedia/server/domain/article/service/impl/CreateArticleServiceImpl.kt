package io.github.wntopia.gikipedia.server.domain.article.service.impl

import io.github.wntopia.gikipedia.server.domain.article.dto.request.CreateArticleReqDto
import io.github.wntopia.gikipedia.server.domain.article.dto.response.ArticleResDto
import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleJpaEntity
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.domain.article.service.ARTICLE_IMAGE_KEY_PREFIX
import io.github.wntopia.gikipedia.server.domain.article.service.ArticleCacheStore
import io.github.wntopia.gikipedia.server.domain.article.service.CreateArticleService
import io.github.wntopia.gikipedia.server.domain.history.event.ArticleUpdatedEvent
import io.github.wntopia.gikipedia.server.domain.link.service.ArticleLinkIndexer
import io.github.wntopia.gikipedia.server.domain.user.service.ContributionRecorder
import io.github.wntopia.gikipedia.server.global.security.session.AuthenticationReader
import io.github.wntopia.gikipedia.server.global.storage.SeaweedUploader
import io.github.wntopia.gikipedia.server.global.text.TitleNormalizer
import jakarta.servlet.http.HttpSession
import org.springframework.context.ApplicationEventPublisher
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import team.themoment.sdk.exception.ExpectedException

@Service
class CreateArticleServiceImpl(
    private val articleRepository: ArticleRepository,
    private val seaweedUploader: SeaweedUploader,
    private val eventPublisher: ApplicationEventPublisher,
    private val articleCacheStore: ArticleCacheStore,
    private val authenticationReader: AuthenticationReader,
    private val contributionRecorder: ContributionRecorder,
    private val articleLinkIndexer: ArticleLinkIndexer,
) : CreateArticleService {
    @Transactional
    override fun execute(
        reqDto: CreateArticleReqDto,
        session: HttpSession,
    ): ArticleResDto {
        val title = TitleNormalizer.normalize(reqDto.title)
        if (title.isEmpty() || articleRepository.existsByTitle(title)) throw duplicateTitle()

        // 비로그인 생성도 허용하므로 로그인한 경우에만 생성자를 기록한다.
        val createdByUserId = authenticationReader.getCurrentUser(session)?.id
        val imageUrl =
            reqDto.image?.takeIf { !it.isEmpty }?.let {
                seaweedUploader.upload(
                    it,
                    ARTICLE_IMAGE_KEY_PREFIX,
                )
            }
        // 사전 확인과 저장 사이에 같은 제목이 끼어드는 경합은 유니크 제약이 막는다 — 즉시 flush해 여기서 409로 변환한다.
        val saved =
            try {
                articleRepository.saveAndFlush(
                    ArticleJpaEntity(title, reqDto.content, imageUrl, createdByUserId = createdByUserId),
                )
            } catch (e: DataIntegrityViolationException) {
                throw duplicateTitle()
            }

        contributionRecorder.recordCreate(createdByUserId, requireNotNull(saved.id))
        articleLinkIndexer.reindex(requireNotNull(saved.id), saved.title, saved.content)

        // article_histories의 리비전 체계("리비전 1 = 생성 시점 그대로")와 맞춰, Mongo 뷰도 생성 시점에
        // 리비전 1로 채워둔다. 이걸 빼먹으면 한 번도 수정 안 된 문서는 Mongo에 영영 없는 상태로 남는다.
        eventPublisher.publishEvent(
            ArticleUpdatedEvent(
                articleId = requireNotNull(saved.id),
                revision = BASELINE_REVISION,
                title = saved.title,
                content = saved.content,
                imageUrl = saved.imageUrl,
                createdAt = requireNotNull(saved.createdAt) { "saved.createdAt이 null입니다 (articleId=${saved.id})" },
                updatedAt = requireNotNull(saved.updatedAt) { "saved.updatedAt이 null입니다 (articleId=${saved.id})" },
            ),
        )

        val response = ArticleResDto.from(saved)
        articleCacheStore.putAfterCommit(response)
        return response
    }

    private fun duplicateTitle() = ExpectedException("이미 존재하는 제목입니다.", HttpStatus.CONFLICT)

    companion object {
        private const val BASELINE_REVISION = 1
    }
}
