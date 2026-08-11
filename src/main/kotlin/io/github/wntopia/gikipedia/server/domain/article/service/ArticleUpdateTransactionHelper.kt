package io.github.wntopia.gikipedia.server.domain.article.service

import io.github.wntopia.gikipedia.server.domain.article.dto.response.ArticleResDto
import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleJpaEntity
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.domain.history.service.ArticleHistoryRecorder
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import team.themoment.sdk.exception.ExpectedException

/**
 * article 수정 경로 세 곳(`UpdateArticleServiceImpl`, `UpdateArticleImageServiceImpl`,
 * `SaveCollaborativeRevisionService`)이 공통으로 반복하던 "행 잠금 조회(404 처리 포함) → 필드 변경 → 리비전
 * 기록 → 커밋 후 캐시 반영" 시퀀스를 하나로 묶는다. 호출 측의 활성 트랜잭션(`@Transactional` 또는
 * `TransactionTemplate`) 안에서 호출되어야 한다.
 *
 * `editorProvider`를 값이 아니라 지연 평가되는 람다로 받는 이유: 편집자 조회(인증 실패 시 401)를
 * `findByIdForUpdate`(존재하지 않으면 404) 이후로 미뤄서, "존재하지 않는 글" 404가 "인증 필요" 401보다
 * 항상 먼저 판정되게 하기 위함이다.
 */
@Component
class ArticleUpdateTransactionHelper(
    private val articleRepository: ArticleRepository,
    private val articleHistoryRecorder: ArticleHistoryRecorder,
    private val articleCacheStore: ArticleCacheStore,
) {
    fun update(
        articleId: Long,
        editorProvider: () -> String,
        mutate: (ArticleJpaEntity) -> Pair<String, String>,
    ): Pair<ArticleResDto, Int?> {
        val article =
            articleRepository.findByIdForUpdate(articleId)
                ?: throw ExpectedException("존재하지 않는 게시글입니다.", HttpStatus.NOT_FOUND)

        val editor = editorProvider()
        val (before, after) = mutate(article)
        val revision = articleHistoryRecorder.record(article, before, after, editor)

        val response = ArticleResDto.from(article)
        articleCacheStore.putAfterCommit(response)
        return response to revision
    }
}
