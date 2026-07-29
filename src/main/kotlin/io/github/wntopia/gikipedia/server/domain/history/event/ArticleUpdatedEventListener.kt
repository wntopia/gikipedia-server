package io.github.wntopia.gikipedia.server.domain.history.event

import io.github.wntopia.gikipedia.server.domain.article.service.ArticleMongoSynchronizer
import org.springframework.modulith.events.ApplicationModuleListener
import org.springframework.stereotype.Component

/**
 * 문서 생성/수정 트랜잭션 커밋 이후 MongoDB 최신 문서 뷰를 동기화한다.
 *
 * `@ApplicationModuleListener`는 `@Async` + `@Transactional(REQUIRES_NEW)` + Spring Modulith의
 * Event Publication Registry 등록을 합친 것이다. 이벤트 발행 시점에 발행 기록이 영속화되고, 이 메서드가
 * 예외 없이 끝나야 완료 처리된다 — 실패하면 기록이 미완료로 남아 앱 재시작 시 자동 드레인되고,
 * [io.github.wntopia.gikipedia.server.domain.article.service.ArticleEventPublicationRetryScheduler]가
 * 주기적으로 재시도한다.
 */
@Component
class ArticleUpdatedEventListener(
    private val articleMongoSynchronizer: ArticleMongoSynchronizer,
) {
    @ApplicationModuleListener
    fun syncLatestView(event: ArticleUpdatedEvent) {
        articleMongoSynchronizer.sync(event.articleId, event.revision)
    }
}
