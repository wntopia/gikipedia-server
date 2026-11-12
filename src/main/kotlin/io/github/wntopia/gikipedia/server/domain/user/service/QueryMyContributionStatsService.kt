package io.github.wntopia.gikipedia.server.domain.user.service

import io.github.wntopia.gikipedia.server.domain.user.dto.response.ContributionStatsResDto
import jakarta.servlet.http.HttpSession

interface QueryMyContributionStatsService {
    fun execute(session: HttpSession): ContributionStatsResDto
}
