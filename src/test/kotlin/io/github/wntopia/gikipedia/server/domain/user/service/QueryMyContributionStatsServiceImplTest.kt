package io.github.wntopia.gikipedia.server.domain.user.service

import io.github.wntopia.gikipedia.server.domain.auth.dto.response.DataGsmUserInfoResDto
import io.github.wntopia.gikipedia.server.domain.user.entity.ContributionType
import io.github.wntopia.gikipedia.server.domain.user.entity.UserJpaEntity
import io.github.wntopia.gikipedia.server.domain.user.repository.ContributionRepository
import io.github.wntopia.gikipedia.server.domain.user.service.impl.QueryMyContributionStatsServiceImpl
import io.github.wntopia.gikipedia.server.global.security.session.AuthenticationReader
import jakarta.servlet.http.HttpSession
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import team.themoment.sdk.exception.ExpectedException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class QueryMyContributionStatsServiceImplTest {
    private val authenticationReader = mock<AuthenticationReader>()
    private val userRegistrar = mock<UserRegistrar>()
    private val contributionRepository = mock<ContributionRepository>()
    private val session = mock<HttpSession>()

    // UTC 2026-10-12 16:00 = KST 2026-10-13 01:00 → 한국 시간 "오늘"은 UTC 10-12 15:00부터
    private val clock = Clock.fixed(Instant.parse("2026-10-12T16:00:00Z"), ZoneOffset.UTC)
    private val service =
        QueryMyContributionStatsServiceImpl(authenticationReader, userRegistrar, contributionRepository, clock)

    private val userInfo = DataGsmUserInfoResDto(10L, "a@gsm.hs.kr", "USER", true, null)

    @Test
    @DisplayName("로그인하지 않았으면 401을 던진다")
    fun requiresLogin() {
        whenever(authenticationReader.getCurrentUser(session)).thenReturn(null)

        assertThatThrownBy { service.execute(session) }
            .isInstanceOfSatisfying(ExpectedException::class.java) {
                assertThat(it.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
            }
    }

    @Test
    @DisplayName("한국 시간 자정을 기준으로 오늘 편집 수를 센다")
    fun countsTodayFromKstMidnight() {
        val kstMidnight = Instant.parse("2026-10-12T15:00:00Z")
        whenever(authenticationReader.getCurrentUser(session)).thenReturn(userInfo)
        whenever(userRegistrar.register(userInfo)).thenReturn(UserJpaEntity(10L, "홍길동", "USER"))
        whenever(contributionRepository.countByUserIdAndType(10L, ContributionType.EDIT)).thenReturn(42)
        whenever(contributionRepository.countByUserIdAndType(10L, ContributionType.CREATE)).thenReturn(3)
        whenever(
            contributionRepository.countByUserIdAndTypeAndCreatedAtGreaterThanEqual(
                10L,
                ContributionType.EDIT,
                kstMidnight,
            ),
        ).thenReturn(2)

        val stats = service.execute(session)

        assertThat(stats.totalEdits).isEqualTo(42)
        assertThat(stats.createdArticles).isEqualTo(3)
        assertThat(stats.todayEdits).isEqualTo(2)
    }
}
