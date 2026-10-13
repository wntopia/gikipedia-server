package io.github.wntopia.gikipedia.server.domain.article.service

import io.github.wntopia.gikipedia.server.domain.article.dto.response.RecentChangeResDto
import io.github.wntopia.gikipedia.server.global.dto.PageResDto

interface QueryRecentChangesService {
    fun execute(
        page: Int,
        size: Int,
    ): PageResDto<RecentChangeResDto>
}
