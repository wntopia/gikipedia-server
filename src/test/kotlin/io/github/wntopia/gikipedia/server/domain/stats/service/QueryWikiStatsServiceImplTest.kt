package io.github.wntopia.gikipedia.server.domain.stats.service

import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.domain.stats.service.impl.QueryWikiStatsServiceImpl
import io.github.wntopia.gikipedia.server.domain.user.repository.ContributionRepository
import io.github.wntopia.gikipedia.server.domain.user.repository.UserRepository
import io.github.wntopia.gikipedia.server.global.config.WikiStatsEnvironment
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class QueryWikiStatsServiceImplTest {
    private val articleRepository = mock<ArticleRepository>()
    private val contributionRepository = mock<ContributionRepository>()
    private val userRepository = mock<UserRepository>()
    private val environment = WikiStatsEnvironment(cacheSeconds = 60)

    /** 시각을 테스트에서 직접 움직일 수 있는 시계. */
    private class MutableClock(
        var now: Instant,
    ) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId): Clock = this

        override fun instant(): Instant = now
    }

    // 2026-10-14(수) 12:00 UTC = KST 21:00 → 이번 주 시작은 월요일 2026-10-12 00:00 KST = 2026-10-11T15:00Z
    private val clock = MutableClock(Instant.parse("2026-10-14T12:00:00Z"))
    private val service =
        QueryWikiStatsServiceImpl(articleRepository, contributionRepository, userRepository, environment, clock)

    @BeforeEach
    fun setUp() {
        whenever(articleRepository.count()).thenReturn(128)
        whenever(userRepository.count()).thenReturn(54)
        whenever(
            contributionRepository.countByCreatedAtGreaterThanEqual(Instant.parse("2026-10-11T15:00:00Z")),
        ).thenReturn(37)
    }

    @Test
    @DisplayName("세 값을 한 번에 반환하고, 이번 주는 한국 시간 월요일 0시부터 센다")
    fun returnsThreeValues() {
        val stats = service.execute()

        assertThat(stats.totalArticles).isEqualTo(128)
        assertThat(stats.weeklyEdits).isEqualTo(37)
        assertThat(stats.registeredUsers).isEqualTo(54)
    }

    @Test
    @DisplayName("캐시 주기 안에는 다시 집계하지 않고, 주기가 지나면 새 값을 반영한다")
    fun cachesWithinWindowAndRefreshesAfter() {
        service.execute()
        clock.now = clock.now.plusSeconds(30)
        whenever(articleRepository.count()).thenReturn(129)

        assertThat(service.execute().totalArticles).isEqualTo(128)
        verify(articleRepository, times(1)).count()

        clock.now = clock.now.plusSeconds(31)
        assertThat(service.execute().totalArticles).isEqualTo(129)
    }
}
