package io.github.wntopia.gikipedia.server.domain.history.event

/**
 * 문서가 수정되어 새 리비전이 커밋된 뒤 발행되는 이벤트.
 *
 * RDB 트랜잭션 커밋 이후(AFTER_COMMIT) 리스너가 이 이벤트를 받아 MongoDB의 최신 문서 뷰([io.github.wntopia.gikipedia.server.domain.article.entity.ArticleMongoEntity])를
 * 동기화한다. Mongo 반영 실패가 문서 수정 자체를 막지 않도록(eventual consistency) 트랜잭션 밖에서 처리한다.
 */
data class ArticleUpdatedEvent(
    val articleId: Long,
    val revision: Int,
    val title: String,
    val content: String,
)
