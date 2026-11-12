package io.github.wntopia.gikipedia.server.domain.stats.service

import io.github.wntopia.gikipedia.server.domain.stats.dto.response.WikiStatsResDto

interface QueryWikiStatsService {
    fun execute(): WikiStatsResDto
}
