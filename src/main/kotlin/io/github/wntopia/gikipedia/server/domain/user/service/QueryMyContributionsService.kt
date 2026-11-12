package io.github.wntopia.gikipedia.server.domain.user.service

import io.github.wntopia.gikipedia.server.domain.user.dto.response.ContributionResDto
import jakarta.servlet.http.HttpSession

interface QueryMyContributionsService {
    fun execute(
        session: HttpSession,
        limit: Int,
    ): List<ContributionResDto>
}
