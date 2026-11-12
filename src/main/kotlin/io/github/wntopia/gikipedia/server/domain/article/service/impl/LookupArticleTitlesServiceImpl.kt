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
        // 정규화 이전에 저장된 제목도 찾을 수 있도록 원문도 함께 조회한다. 응답은 정규화 키로 맞추므로 원문으로 찾은
        // 문서도 같은 제목의 요청에 연결된다.
        val normalizedTitles =
            (reqDto.titles.map(TitleNormalizer::normalize) + reqDto.titles)
                // 컬럼 길이를 넘는 제목은 존재할 수 없으므로 조회하지 않는다(긴 입력이 그대로 IN 절에 들어가지 않도록).
                .filter { it.isNotBlank() && it.length <= TitleNormalizer.MAX_LENGTH }
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
