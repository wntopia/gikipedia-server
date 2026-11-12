package io.github.wntopia.gikipedia.server.domain.link.service

import io.github.wntopia.gikipedia.server.domain.link.dto.response.BacklinkResDto
import io.github.wntopia.gikipedia.server.global.dto.PageResDto

interface QueryBacklinksService {
    fun execute(
        title: String,
        page: Int,
        size: Int,
    ): PageResDto<BacklinkResDto>
}
