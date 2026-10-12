package io.github.wntopia.gikipedia.server.domain.article.service.impl

import io.github.wntopia.gikipedia.server.domain.article.dto.request.LookupArticleTitlesReqDto
import io.github.wntopia.gikipedia.server.domain.article.dto.response.LookupArticleTitlesResDto
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.domain.article.service.LookupArticleTitlesService
import io.github.wntopia.gikipedia.server.global.text.TitleNormalizer
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LookupArticleTitlesServiceImpl(
    private val articleRepository: ArticleRepository,
) : LookupArticleTitlesService {
    @Transactional(readOnly = true)
    override fun execute(reqDto: LookupArticleTitlesReqDto): LookupArticleTitlesResDto {
        val normalizedTitles =
            reqDto.titles
                .map(TitleNormalizer::normalize)
                .filter { it.isNotEmpty() }
                .distinct()
        val idByKey =
            if (normalizedTitles.isEmpty()) {
                emptyMap()
            } else {
                articleRepository
                    .findIdAndTitleByTitleIn(normalizedTitles)
                    .associate { TitleNormalizer.keyOf(it.title) to it.id }
            }

        return LookupArticleTitlesResDto(
            reqDto.titles.map { title ->
                val id = idByKey[TitleNormalizer.keyOf(title)]
                LookupArticleTitlesResDto.Item(title = title, exists = id != null, id = id)
            },
        )
    }
}
