package io.github.wntopia.gikipedia.server.domain.collaboration.service

import io.github.wntopia.gikipedia.server.domain.article.dto.response.ArticleResDto
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.domain.article.service.ArticleCacheStore
import io.github.wntopia.gikipedia.server.domain.history.service.ArticleHistoryRecorder
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import team.themoment.sdk.exception.ExpectedException

/**
 * 실시간 공동편집(WebSocket)에서 오는 저장 요청을 기존 리비전 파이프라인([ArticleHistoryRecorder])에
 * 연결한다. `UpdateArticleServiceImpl`과 같은 행 잠금·기록 순서를 따르되, 이미지/멀티파트가 없는 축약판이다.
 *
 * 명시적 저장(`save`) 메시지와 마지막 참여자 퇴장 시 안전망 저장이 모두 이 서비스를 재사용한다.
 * `articleHistoryRecorder.record`는 변경 없음(diff 없음)도 안전하게 처리하므로(리비전 생성 없이 이벤트만
 * 재발행), 안전망이 이미 저장된 것과 같은 내용을 다시 호출해도 멱등적으로 무해하다.
 */
@Service
class SaveCollaborativeRevisionService(
    private val articleRepository: ArticleRepository,
    private val articleHistoryRecorder: ArticleHistoryRecorder,
    private val articleCacheStore: ArticleCacheStore,
) {
    @Transactional
    fun save(
        articleId: Long,
        plainText: String,
        editor: String,
    ): Int? {
        val article =
            articleRepository.findByIdForUpdate(articleId)
                ?: throw ExpectedException("존재하지 않는 게시글입니다.", HttpStatus.NOT_FOUND)
        val previousContent = article.content
        article.update(plainText, article.imageUrl)
        val revision = articleHistoryRecorder.record(article, previousContent, plainText, editor)
        articleCacheStore.putAfterCommit(ArticleResDto.from(article))
        return revision
    }
}
