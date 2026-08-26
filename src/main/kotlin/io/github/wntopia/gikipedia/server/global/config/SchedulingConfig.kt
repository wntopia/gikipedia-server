package io.github.wntopia.gikipedia.server.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler

@Configuration
@EnableScheduling
class SchedulingConfig {
    @Suppress("UsePropertyAccessSyntax")
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
