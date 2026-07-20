package io.github.wntopia.gikipedia.server.domain.article.service.impl

import io.github.wntopia.gikipedia.server.domain.article.dto.request.UpdateArticleReqDto
import io.github.wntopia.gikipedia.server.domain.article.dto.response.ArticleResDto
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.domain.article.service.UpdateArticleService
import io.github.wntopia.gikipedia.server.domain.history.service.ArticleHistoryRecorder
import io.github.wntopia.gikipedia.server.global.security.session.AuthenticationReader
import io.github.wntopia.gikipedia.server.global.storage.R2Uploader
import jakarta.servlet.http.HttpSession
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import team.themoment.sdk.exception.ExpectedException

@Service
class UpdateArticleServiceImpl(
    private val articleRepository: ArticleRepository,
    private val r2Uploader: R2Uploader,
    private val articleHistoryRecorder: ArticleHistoryRecorder,
    private val authenticationReader: AuthenticationReader,
) : UpdateArticleService {
    @Transactional
    override fun execute(
        articleId: Long,
        reqDto: UpdateArticleReqDto,
        session: HttpSession,
    ): ArticleResDto {
        val article =
            articleRepository
                .findById(articleId)
                .orElseThrow { ExpectedException("존재하지 않는 게시글입니다.", HttpStatus.NOT_FOUND) }

        val editor = authenticationReader.getEditorLabel(session)
        val previousContent = article.content

        val imageUrl =
            reqDto.image?.takeIf { !it.isEmpty }?.let { r2Uploader.upload(it, IMAGE_KEY_PREFIX) }
                ?: article.imageUrl

        article.update(reqDto.title, reqDto.content, imageUrl)

        // diff/스냅샷 기록과 최신 스냅샷 갱신은 하나의 트랜잭션으로 원자 커밋된다.
        // Mongo 동기화는 recorder가 발행하는 이벤트를 AFTER_COMMIT 리스너가 처리한다.
        articleHistoryRecorder.record(article, previousContent, reqDto.content, editor)

        return ArticleResDto.from(article)
    }

    companion object {
        private const val IMAGE_KEY_PREFIX = "articles"
    }
}
