package io.github.wntopia.gikipedia.server.domain.user.service.impl

import io.github.wntopia.gikipedia.server.domain.user.dto.response.ContributionResDto
import io.github.wntopia.gikipedia.server.domain.user.repository.ContributionRepository
import io.github.wntopia.gikipedia.server.domain.user.service.QueryMyContributionsService
import io.github.wntopia.gikipedia.server.domain.user.service.UserRegistrar
import io.github.wntopia.gikipedia.server.global.security.session.AuthenticationReader
import jakarta.servlet.http.HttpSession
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import team.themoment.sdk.exception.ExpectedException

@Service
class QueryMyContributionsServiceImpl(
    private val authenticationReader: AuthenticationReader,
    private val userRegistrar: UserRegistrar,
    private val contributionRepository: ContributionRepository,
) : QueryMyContributionsService {
    override fun execute(
        session: HttpSession,
        limit: Int,
    ): List<ContributionResDto> {
        val userInfo =
            authenticationReader.getCurrentUser(session)
                ?: throw ExpectedException("인증이 필요합니다.", HttpStatus.UNAUTHORIZED)
        val user =
            userRegistrar.findOrRegister(userInfo) ?: throw ExpectedException("인증이 필요합니다.", HttpStatus.UNAUTHORIZED)
        return contributionRepository
            .findRecentByUserId(user.datagsmUserId, PageRequest.of(0, limit))
            .map(ContributionResDto::from)
    }
}
