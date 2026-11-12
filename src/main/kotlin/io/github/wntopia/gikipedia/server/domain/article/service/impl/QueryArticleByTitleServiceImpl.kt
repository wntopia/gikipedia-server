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
 *
 * 제목 정규화 규칙 도입 이전에 만들어진 문서는 제목이 정규화되지 않은 채 저장돼 있을 수 있다. 제목은 만든 뒤 바꾸지
 * 않으므로 데이터를 고치는 대신, 정규화한 제목으로 못 찾으면 요청 원문 그대로 한 번 더 찾는다.
 */
@Service
class QueryArticleByTitleServiceImpl(
    private val articleRepository: ArticleRepository,
    private val queryArticleService: QueryArticleService,
) : QueryArticleByTitleService {
    override fun execute(title: String): ArticleResDto {
        // 정규화로 줄어들 공백을 감안해도 이보다 긴 입력은 존재할 수 없는 제목이라, 긴 문자열로 DB를 조회하지 않는다.
        if (title.length > TitleNormalizer.MAX_LENGTH * 2) throw notFound()
        val normalized = TitleNormalizer.normalize(title)
        val articleId =
            (
                articleRepository.findByTitle(normalized)
                    ?: title.takeIf { it != normalized }?.let(articleRepository::findByTitle)
            )?.id
                ?: throw notFound()
        return queryArticleService.execute(articleId)
    }

    private fun notFound() = ExpectedException("존재하지 않는 게시글입니다.", HttpStatus.NOT_FOUND)
}
