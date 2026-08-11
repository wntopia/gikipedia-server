package io.github.wntopia.gikipedia.server.domain.collaboration.event

import io.github.wntopia.gikipedia.server.domain.collaboration.model.CollaborationMessage
import io.github.wntopia.gikipedia.server.domain.collaboration.service.CollaborationRoomRegistry
import io.github.wntopia.gikipedia.server.domain.history.event.ArticleUpdatedEvent
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * PUT(비대화형 API 전용)이나 이미지 전용 업로드로 room 밖에서 글이 바뀌었을 때, 그 article에 활성 공동편집
 * room이 있으면 현재 접속 중인 클라이언트에게 알린다. 새 이벤트를 만들지 않고 모든 수정 경로가 이미 발행하는
 * [ArticleUpdatedEvent]를 그대로 재사용한다.
 *
 * Mongo 동기화 리스너([io.github.wntopia.gikipedia.server.domain.history.event.ArticleUpdatedEventListener])와
 * 달리 `@ApplicationModuleListener`가 아니라 `@TransactionalEventListener`를 쓴다 — 이 브로드캐스트는 "지금
 * 연결된 WS 클라이언트에게 보내는 best-effort 알림"일 뿐이라, Modulith의 영속화된 재시도/재발행 시맨틱이
 * 적용되면 안 된다(앱 재시작 후 오래된 이벤트를 재생해 "방금 바뀜"이라고 알리는 건 의미가 없다).
 */
@Component
class CollaborationExternalUpdateListener(
    private val collaborationRoomRegistry: CollaborationRoomRegistry,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onArticleUpdated(event: ArticleUpdatedEvent) {
        collaborationRoomRegistry.findIfActive(event.articleId)?.broadcast(
            CollaborationMessage(
                type = CollaborationMessage.TYPE_EXTERNAL_UPDATE,
                content = event.content,
                imageUrl = event.imageUrl,
            ),
        )
    }
}
