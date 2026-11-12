package io.github.wntopia.gikipedia.server.domain.stats.service.impl

import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.domain.stats.dto.response.WikiStatsResDto
import io.github.wntopia.gikipedia.server.domain.stats.service.QueryWikiStatsService
import io.github.wntopia.gikipedia.server.domain.user.repository.ContributionRepository
import io.github.wntopia.gikipedia.server.domain.user.repository.UserRepository
import io.github.wntopia.gikipedia.server.global.config.WikiStatsEnvironment
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.atomic.AtomicReference

/**
 * 세 값을 집계하는 COUNT 쿼리 비용을 줄이려고, 마지막 집계 결과를 [WikiStatsEnvironment.cacheSeconds] 동안 재사용한다.
 * 만료 직후 동시에 요청이 몰리면 각자 한 번씩 집계할 수 있지만, 값이 같아 결과에는 문제가 없다.
 */
@Service
class QueryWikiStatsServiceImpl(
    private val articleRepository: ArticleRepository,
    private val contributionRepository: ContributionRepository,
    private val userRepository: UserRepository,
    private val environment: WikiStatsEnvironment,
    private val clock: Clock,
) : QueryWikiStatsService {
    private data class Cached(
        val stats: WikiStatsResDto,
        val computedAt: Instant,
    )

    private val cached = AtomicReference<Cached?>(null)

    override fun execute(): WikiStatsResDto {
        val now = clock.instant()
        cached.get()?.let {
            if (Duration.between(it.computedAt, now) < Duration.ofSeconds(environment.cacheSeconds)) return it.stats
        }

        val stats =
            WikiStatsResDto(
                totalArticles = articleRepository.count(),
                weeklyEdits = contributionRepository.countByCreatedAtGreaterThanEqual(startOfThisWeek()),
                registeredUsers = userRepository.count(),
            )
        cached.set(Cached(stats, now))
        return stats
    }

    private fun startOfThisWeek(): Instant =
        LocalDate
            .now(clock.withZone(KST))
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .atStartOfDay(KST)
            .toInstant()

    companion object {
        private val KST: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
