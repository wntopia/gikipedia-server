package io.github.wntopia.gikipedia.server.domain.article.service.impl

import io.github.wntopia.gikipedia.server.domain.article.dto.request.UpdateArticleReqDto
import io.github.wntopia.gikipedia.server.domain.article.dto.response.ArticleResDto
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.domain.article.service.ArticleCacheStore
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
    private val articleCacheStore: ArticleCacheStore,
) : UpdateArticleService {
    @Transactional
    override fun execute(
        articleId: Long,
        reqDto: UpdateArticleReqDto,
        session: HttpSession,
    ): ArticleResDto {
        // R2 업로드(블로킹 네트워크 호출)는 아래 행 잠금을 잡기 전에 끝내둔다 — 그래야 느린 업로드가
        // 같은 article을 동시에 수정하려는 다른 요청까지 잠가버리는 일이 없다.
        val uploadedImageUrl = reqDto.image?.takeIf { !it.isEmpty }?.let { r2Uploader.upload(it, IMAGE_KEY_PREFIX) }

        // 같은 article에 대한 동시 수정 요청을 여기서 직렬화한다 — 두 트랜잭션이 article_histories의
        // "다음 리비전 번호"를 동시에 같은 값으로 계산해 유니크 제약 위반으로 한쪽이 실패하는 경합을 막는다.
        val article =
            articleRepository.findByIdForUpdate(articleId)
                ?: throw ExpectedException("존재하지 않는 게시글입니다.", HttpStatus.NOT_FOUND)

        val editor = authenticationReader.getEditorLabel(session)
        val previousContent = article.content
        val imageUrl = uploadedImageUrl ?: article.imageUrl

        article.update(reqDto.content, imageUrl)

        // diff/스냅샷 기록과 최신 스냅샷 갱신은 하나의 트랜잭션으로 원자 커밋된다.
        // Mongo 동기화는 recorder가 발행하는 이벤트를 Modulith 리스너가 커밋 이후 처리한다.
        articleHistoryRecorder.record(article, previousContent, reqDto.content, editor)

        val response = ArticleResDto.from(article)
        articleCacheStore.putAfterCommit(response)
        return response
    }

    companion object {
        private const val IMAGE_KEY_PREFIX = "articles"
    }
}
