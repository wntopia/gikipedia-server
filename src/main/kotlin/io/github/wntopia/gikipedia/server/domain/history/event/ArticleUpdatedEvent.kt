package io.github.wntopia.gikipedia.server.domain.history.event

import java.time.Instant

/**
 * 문서가 생성되었거나 수정되어 새 리비전이 커밋된 뒤 발행되는 이벤트. 생성 시에는 revision=1(baseline)으로 발행된다.
 *
 * Spring Modulith의 Event Publication Registry가 발행 시점에 이 이벤트를 영속화하고, 리스너가 성공적으로 처리할 때까지 재시도를 보장한다.
 * Mongo 동기화에 필요한 데이터를 전부 담아서, 리스너가 이 이벤트만으로 MongoDB를 채울 수 있게 한다(MySQL 재조회 없음).
 */
data class ArticleUpdatedEvent(
    val articleId: Long,
    val revision: Int,
    val title: String,
    val content: String,
    val imageUrl: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
