package io.github.wntopia.gikipedia.server.domain.collaboration.service

import io.github.wntopia.gikipedia.server.domain.article.service.ArticleUpdateTransactionHelper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 실시간 공동편집(WebSocket)에서 오는 저장 요청을 기존 리비전 파이프라인에 연결한다.
 * [ArticleUpdateTransactionHelper]가 `UpdateArticleServiceImpl`과 같은 행 잠금·기록 순서를 담당하며,
 * 여기서는 이미지/멀티파트가 없는 축약판으로 content만 넘긴다.
 *
 * 명시적 저장(`save`) 메시지와 마지막 참여자 퇴장 시 안전망 저장이 모두 이 서비스를 재사용한다.
 * `record`는 변경 없음(diff 없음)도 안전하게 처리하므로(리비전 생성 없이 이벤트만 재발행), 안전망이 이미
 * 저장된 것과 같은 내용을 다시 호출해도 멱등적으로 무해하다.
 */
@Service
class SaveCollaborativeRevisionService(
    private val articleUpdateTransactionHelper: ArticleUpdateTransactionHelper,
) {
    @Transactional
    fun save(
        articleId: Long,
        plainText: String,
        editor: String,
    ): Int? {
        val (_, revision) =
            articleUpdateTransactionHelper.update(
                articleId = articleId,
                editorProvider = { editor },
            ) { article ->
                val previousContent = article.content
                article.update(plainText, article.imageUrl)
                previousContent to plainText
            }
        return revision
    }
}
