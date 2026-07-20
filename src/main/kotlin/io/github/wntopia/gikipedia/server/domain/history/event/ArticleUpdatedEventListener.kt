package io.github.wntopia.gikipedia.server.domain.history.event

import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleMongoEntity
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleMongoRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 문서 수정 트랜잭션 커밋 이후 MongoDB 최신 문서 뷰를 동기화한다.
 *
 * AFTER_COMMIT에서 실행되므로 RDB에는 이미 새 리비전이 확정된 상태다. Mongo 반영이 실패해도 문서 수정 자체는 롤백되지 않으며(RDB가 진실의 원천), 실패는 로깅만 하고 넘어간다 — 뷰는 언제든
 * 재구성으로 복구할 수 있다. 누락 감지·자동 복구를 강화하려면 아웃박스/큐 패턴으로 확장한다(로드맵 참조).
 */
@Component
class ArticleUpdatedEventListener(
    private val articleMongoRepository: ArticleMongoRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun syncLatestView(event: ArticleUpdatedEvent) {
        try {
            val existing = articleMongoRepository.findByDocumentId(event.articleId)
            existing?.let { articleMongoRepository.delete(it) }
            articleMongoRepository.save(
                ArticleMongoEntity(
                    documentId = event.articleId,
                    title = event.title,
                    content = event.content,
                    revision = event.revision,
                ),
            )
        } catch (e: Exception) {
            log.error("Mongo 최신 뷰 동기화 실패 (articleId=${event.articleId}, revision=${event.revision})", e)
        }
    }
}
