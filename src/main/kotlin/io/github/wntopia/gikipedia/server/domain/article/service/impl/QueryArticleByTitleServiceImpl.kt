package io.github.wntopia.gikipedia.server.domain.article.service.impl

import io.github.wntopia.gikipedia.server.domain.article.dto.response.ArticleResDto
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.domain.article.service.QueryArticleByTitleService
import io.github.wntopia.gikipedia.server.domain.article.service.QueryArticleService
import io.github.wntopia.gikipedia.server.global.text.TitleNormalizer
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import team.themoment.sdk.exception.ExpectedException

/**
 * 제목 → id 해석은 MySQL의 제목 유니크 인덱스로 하고, 본문 조회는 id 기반 [QueryArticleService]에 맡긴다.
 * 그러면 읽기 소스(MySQL / Redis·Mongo) 설정이 제목 조회에도 그대로 적용된다.
 */
@Service
class QueryArticleByTitleServiceImpl(
    private val articleRepository: ArticleRepository,
    private val queryArticleService: QueryArticleService,
) : QueryArticleByTitleService {
    override fun execute(title: String): ArticleResDto {
        val articleId =
            articleRepository.findByTitle(TitleNormalizer.normalize(title))?.id
                ?: throw ExpectedException("존재하지 않는 게시글입니다.", HttpStatus.NOT_FOUND)
        return queryArticleService.execute(articleId)
    }
}
