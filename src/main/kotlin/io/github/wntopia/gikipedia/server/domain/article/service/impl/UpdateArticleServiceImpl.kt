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
import org.springframework.transaction.support.TransactionTemplate
import team.themoment.sdk.exception.ExpectedException

@Service
class UpdateArticleServiceImpl(
    private val articleRepository: ArticleRepository,
    private val r2Uploader: R2Uploader,
    private val articleHistoryRecorder: ArticleHistoryRecorder,
    private val authenticationReader: AuthenticationReader,
    private val articleCacheStore: ArticleCacheStore,
    private val transactionTemplate: TransactionTemplate,
) : UpdateArticleService {
    /**
     * R2 업로드(블로킹 네트워크 호출)와 인증 정보 조회는 DB와 무관하므로 트랜잭션 밖에서 끝내둔다.
     * `@Transactional`로 메서드 전체를 감쌌다면 DB 커넥션 획득 시점이 Hibernate의 지연 획득 설정에
     * 암묵적으로 의존하게 되는데다, 업로드는 실패해도 롤백 대상이 아니라 트랜잭션 안에 있을 실익이 없다.
     * 아래 [TransactionTemplate] 블록만 명시적으로 DB 쓰기 트랜잭션으로 묶는다.
     */
    override fun execute(
        articleId: Long,
        reqDto: UpdateArticleReqDto,
        session: HttpSession,
    ): ArticleResDto {
        val uploadedImageUrl = reqDto.image?.takeIf { !it.isEmpty }?.let { r2Uploader.upload(it, IMAGE_KEY_PREFIX) }
        val editor = authenticationReader.getEditorLabel(session)

        return requireNotNull(
            transactionTemplate.execute {
                // 같은 article에 대한 동시 수정 요청을 여기서 직렬화한다 — 두 트랜잭션이 article_histories의
                // "다음 리비전 번호"를 동시에 같은 값으로 계산해 유니크 제약 위반으로 한쪽이 실패하는 경합을 막는다.
                val article =
                    articleRepository.findByIdForUpdate(articleId)
                        ?: throw ExpectedException("존재하지 않는 게시글입니다.", HttpStatus.NOT_FOUND)

                val previousContent = article.content
                val imageUrl = uploadedImageUrl ?: article.imageUrl

                article.update(reqDto.content, imageUrl)

                // diff/스냅샷 기록과 최신 스냅샷 갱신은 하나의 트랜잭션으로 원자 커밋된다.
                // Mongo 동기화는 recorder가 발행하는 이벤트를 Modulith 리스너가 커밋 이후 처리한다.
                articleHistoryRecorder.record(article, previousContent, reqDto.content, editor)

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
