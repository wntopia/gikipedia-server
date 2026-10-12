package io.github.wntopia.gikipedia.server.domain.user.service.impl

import io.github.wntopia.gikipedia.server.domain.user.dto.response.ContributionStatsResDto
import io.github.wntopia.gikipedia.server.domain.user.entity.ContributionType
import io.github.wntopia.gikipedia.server.domain.user.repository.ContributionRepository
import io.github.wntopia.gikipedia.server.domain.user.service.QueryMyContributionStatsService
import io.github.wntopia.gikipedia.server.domain.user.service.UserRegistrar
import io.github.wntopia.gikipedia.server.global.security.session.AuthenticationReader
import jakarta.servlet.http.HttpSession
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import team.themoment.sdk.exception.ExpectedException
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

@Service
class QueryMyContributionStatsServiceImpl(
    private val authenticationReader: AuthenticationReader,
    private val userRegistrar: UserRegistrar,
    private val contributionRepository: ContributionRepository,
    private val clock: Clock,
) : QueryMyContributionStatsService {
    override fun execute(session: HttpSession): ContributionStatsResDto {
        val userInfo =
            authenticationReader.getCurrentUser(session)
                ?: throw ExpectedException("인증이 필요합니다.", HttpStatus.UNAUTHORIZED)
        // 이 기능 배포 전에 로그인한 세션은 사용자 레코드가 아직 없을 수 있어 여기서도 보장한다.
        val user =
            userRegistrar.findOrRegister(userInfo) ?: throw ExpectedException("인증이 필요합니다.", HttpStatus.UNAUTHORIZED)
        val userId = user.datagsmUserId
        val startOfToday = LocalDate.now(clock.withZone(KST)).atStartOfDay(KST).toInstant()

        return ContributionStatsResDto(
            joinedAt = user.createdAt,
            totalEdits = contributionRepository.countByUserIdAndType(userId, ContributionType.EDIT),
            createdArticles = contributionRepository.countByUserIdAndType(userId, ContributionType.CREATE),
            todayEdits =
                contributionRepository.countByUserIdAndTypeAndCreatedAtGreaterThanEqual(
                    userId,
                    ContributionType.EDIT,
                    startOfToday,
                ),
        )
    }

    companion object {
        private val KST: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
