package io.github.wntopia.gikipedia.server.domain.article.service

import org.slf4j.LoggerFactory
import org.springframework.modulith.events.IncompleteEventPublications
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * 처리에 실패해 미완료로 남은 이벤트 발행 기록을 고정 주기로 재시도한다.
 *
 * 앱 재시작 시 자동 드레인(`republish-outstanding-events-on-restart`)만으로는 앱이 오래 떠 있는 동안의
 * 실패를 못 잡으므로 이 백업 폴링이 필요하다. 트래픽 규모상 실패가 드물다고 가정해 지수 백오프 없이
 * 단순 고정 주기로 충분하다고 판단했다.
 */
@Component
class ArticleEventPublicationRetryScheduler(
    private val incompleteEventPublications: IncompleteEventPublications,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = RETRY_INTERVAL_MILLIS)
    fun retryIncompletePublications() {
        log.info("미완료 이벤트 발행 재시도 스캔 시작")
        // MIN_AGE보다 최근 것은 정상 처리 중일 수 있으므로 제외하고, 그보다 오래됐는데도 미완료인 것만 재시도한다.
        incompleteEventPublications.resubmitIncompletePublicationsOlderThan(MIN_AGE)
    }

    companion object {
        private const val RETRY_INTERVAL_MILLIS = 600_000L
        private val MIN_AGE = Duration.ofMinutes(1)
    }
}
