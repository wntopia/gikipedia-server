package io.github.wntopia.gikipedia.server.domain.article.service.impl

import io.github.wntopia.gikipedia.server.domain.article.dto.request.UpdateArticleReqDto
import io.github.wntopia.gikipedia.server.domain.article.dto.response.ArticleResDto
import io.github.wntopia.gikipedia.server.domain.article.service.ARTICLE_IMAGE_KEY_PREFIX
import io.github.wntopia.gikipedia.server.domain.article.service.ArticleUpdateTransactionHelper
import io.github.wntopia.gikipedia.server.domain.article.service.UpdateArticleService
import io.github.wntopia.gikipedia.server.global.security.session.AuthenticationReader
import io.github.wntopia.gikipedia.server.global.storage.R2Uploader
import jakarta.servlet.http.HttpSession
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate

@Service
class UpdateArticleServiceImpl(
    private val r2Uploader: R2Uploader,
    private val authenticationReader: AuthenticationReader,
    private val transactionTemplate: TransactionTemplate,
    private val articleUpdateTransactionHelper: ArticleUpdateTransactionHelper,
) : UpdateArticleService {
    /**
     * R2 업로드(블로킹 네트워크 호출)는 DB와 무관하므로 트랜잭션 밖에서 끝내둔다. `@Transactional`로 메서드
     * 전체를 감쌌다면 DB 커넥션 획득 시점이 Hibernate의 지연 획득 설정에 암묵적으로 의존하게 되는데다, 업로드는
     * 실패해도 롤백 대상이 아니라 트랜잭션 안에 있을 실익이 없다. 아래 [TransactionTemplate] 블록만 명시적으로
     * DB 쓰기 트랜잭션으로 묶는다. 편집자 조회(인증 실패 시 401)는 [ArticleUpdateTransactionHelper]가 행 잠금
     * 조회(404 처리) 이후로 미뤄서 호출하므로, "존재하지 않는 글" 404가 "인증 필요" 401보다 먼저 판정된다.
     */
    override fun execute(
        articleId: Long,
        reqDto: UpdateArticleReqDto,
        session: HttpSession,
    ): ArticleResDto {
        val uploadedImageUrl =
            reqDto.image?.takeIf { !it.isEmpty }?.let {
                r2Uploader.upload(
                    it,
                    ARTICLE_IMAGE_KEY_PREFIX,
                )
            }

        val (response, _) =
            requireNotNull(
                // 같은 article에 대한 동시 수정 요청을 여기서 직렬화한다 — 두 트랜잭션이 article_histories의
                // "다음 리비전 번호"를 동시에 같은 값으로 계산해 유니크 제약 위반으로 한쪽이 실패하는 경합을 막는다.
                transactionTemplate.execute {
                    articleUpdateTransactionHelper.update(
                        articleId = articleId,
                        editorProvider = { authenticationReader.getEditorLabel(session) },
                    ) { article ->
                        val previousContent = article.content
                        val imageUrl = uploadedImageUrl ?: article.imageUrl
                        article.update(reqDto.content, imageUrl)
                        previousContent to reqDto.content
                    }
                },
            )
        return response
    }
}
