package io.github.wntopia.gikipedia.server.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler

/**
 * ArticleEventPublicationRetryScheduler(10분 주기)와 ArticleConsistencyCheckScheduler(매일 04:00)가
 * 스케줄 작업 대상이다. @EnableScheduling만 켜두면 기본 스케줄러는 스레드 1개를 공유하므로, 한쪽이
 * 오래 걸리면(특히 일일 배치) 다른 쪽의 실행 시각이 밀릴 수 있어 풀 크기를 넉넉히 잡아 분리한다.
 */
@Configuration
@EnableScheduling
class SchedulingConfig {
    @Bean
    fun taskScheduler(): ThreadPoolTaskScheduler =
        ThreadPoolTaskScheduler().apply {
            poolSize = SCHEDULER_POOL_SIZE
            setThreadNamePrefix("scheduled-task-")
        }

    companion object {
        private const val SCHEDULER_POOL_SIZE = 2
    }
}
