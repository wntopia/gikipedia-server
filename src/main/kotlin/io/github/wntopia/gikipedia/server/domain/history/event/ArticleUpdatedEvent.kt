package io.github.wntopia.gikipedia.server.domain.history.event

/**
 * 문서가 생성되었거나 수정되어 새 리비전이 커밋된 뒤 발행되는 이벤트. 생성 시에는 revision=0(baseline)으로 발행된다.
 *
 * Spring Modulith의 Event Publication Registry가 발행 시점에 이 이벤트를 영속화하고, 리스너가 성공적으로 처리할 때까지 재시도를 보장한다.
 * title/content 등 실제 데이터는 담지 않고 최소 정보만 담는다 — 리스너가 MySQL을 재조회해 최신값을 쓰므로, 이 이벤트 스키마가 바뀌어도(필드 추가 등)
 * 이미 저장된 과거 payload의 역직렬화가 영향을 받지 않는다.
 */
data class ArticleUpdatedEvent(
    val articleId: Long,
    val revision: Int,
)
