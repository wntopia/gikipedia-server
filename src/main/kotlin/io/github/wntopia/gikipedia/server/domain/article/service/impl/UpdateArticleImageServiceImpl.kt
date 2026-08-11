package io.github.wntopia.gikipedia.server.domain.article.service.impl

import io.github.wntopia.gikipedia.server.domain.article.dto.request.UpdateArticleImageReqDto
import io.github.wntopia.gikipedia.server.domain.article.dto.response.ArticleResDto
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.domain.article.service.ArticleCacheStore
import io.github.wntopia.gikipedia.server.domain.article.service.UpdateArticleImageService
import io.github.wntopia.gikipedia.server.domain.history.service.ArticleHistoryRecorder
import io.github.wntopia.gikipedia.server.global.security.session.AuthenticationReader
import io.github.wntopia.gikipedia.server.global.storage.R2Uploader
import jakarta.servlet.http.HttpSession
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import team.themoment.sdk.exception.ExpectedException

/**
 * 실시간 공동편집 중에도 대표 이미지만 바꿀 수 있는 별도 엔드포인트용 서비스.
 *
 * `UpdateArticleServiceImpl`과 마찬가지로 R2 업로드(DB와 무관한 외부 I/O)는 트랜잭션 밖에서 끝내고, 그
 * 뒤의 행 잠금+DB 쓰기만 [TransactionTemplate]으로 명시적으로 묶는다. content는 건드리지 않는다. content
 * 변화가 없으므로 `articleHistoryRecorder.record`는 리비전을 새로 만들지 않고 이미지가 반영된
 * [io.github.wntopia.gikipedia.server.domain.history.event.ArticleUpdatedEvent]만 재발행한다 — 그 이벤트를
 * [io.github.wntopia.gikipedia.server.domain.collaboration.event.CollaborationExternalUpdateListener]가
 * 구독해 활성 공동편집 room에도 새 이미지를 실시간으로 알린다.
 */
@Service
class UpdateArticleImageServiceImpl(
    private val articleRepository: ArticleRepository,
    private val r2Uploader: R2Uploader,
    private val articleHistoryRecorder: ArticleHistoryRecorder,
    private val authenticationReader: AuthenticationReader,
    private val articleCacheStore: ArticleCacheStore,
    private val transactionTemplate: TransactionTemplate,
) : UpdateArticleImageService {
    override fun execute(
        articleId: Long,
        reqDto: UpdateArticleImageReqDto,
        session: HttpSession,
    ): ArticleResDto {
        val uploadedImageUrl = r2Uploader.upload(reqDto.image, IMAGE_KEY_PREFIX)
        val editor = authenticationReader.getEditorLabel(session)

        return requireNotNull(
            transactionTemplate.execute {
                val article =
                    articleRepository.findByIdForUpdate(articleId)
                        ?: throw ExpectedException("존재하지 않는 게시글입니다.", HttpStatus.NOT_FOUND)

                article.update(article.content, uploadedImageUrl)
                articleHistoryRecorder.record(article, article.content, article.content, editor)

                val response = ArticleResDto.from(article)
                articleCacheStore.putAfterCommit(response)
                response
            },
        )
    }

    companion object {
        private const val IMAGE_KEY_PREFIX = "articles"
    }
}
