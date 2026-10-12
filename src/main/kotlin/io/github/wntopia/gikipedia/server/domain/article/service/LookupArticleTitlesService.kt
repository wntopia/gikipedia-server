package io.github.wntopia.gikipedia.server.domain.article.service

import io.github.wntopia.gikipedia.server.domain.article.dto.request.LookupArticleTitlesReqDto
import io.github.wntopia.gikipedia.server.domain.article.dto.response.LookupArticleTitlesResDto

interface LookupArticleTitlesService {
    fun execute(reqDto: LookupArticleTitlesReqDto): LookupArticleTitlesResDto
}
