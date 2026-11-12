package io.github.wntopia.gikipedia.server.domain.article.service.impl

import com.mongodb.MongoExecutionTimeoutException
import io.github.wntopia.gikipedia.server.domain.article.dto.request.SearchArticleReqDto
import io.github.wntopia.gikipedia.server.domain.article.dto.request.SearchArticleReqDto.Companion.MAX_PAGE_SIZE
import io.github.wntopia.gikipedia.server.domain.article.dto.request.SearchArticleReqDto.Companion.MAX_QUERY_LENGTH
import io.github.wntopia.gikipedia.server.domain.article.dto.response.ArticleSearchItemResDto
import io.github.wntopia.gikipedia.server.domain.article.dto.response.ArticleSearchResDto
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleSearchRepository
import io.github.wntopia.gikipedia.server.domain.article.service.SearchArticleService
import io.github.wntopia.gikipedia.server.domain.article.util.ArticleSearchSnippetBuilder
import io.github.wntopia.gikipedia.server.global.config.ArticleSearchEnvironment
import org.springframework.dao.DataAccessResourceFailureException
import org.springframework.dao.QueryTimeoutException
import org.springframework.data.mongodb.UncategorizedMongoDbException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import team.themoment.sdk.exception.ExpectedException
import java.util.concurrent.Semaphore

@Service
class SearchArticleServiceImpl(
    private val repository: ArticleSearchRepository,
    environment: ArticleSearchEnvironment,
) : SearchArticleService {
    private val permits = Semaphore(environment.maxConcurrentRequests)

    override fun execute(reqDto: SearchArticleReqDto): ArticleSearchResDto {
        val query = reqDto.q.trim()
        if (query.isEmpty() || reqDto.q.codePointCount(0, reqDto.q.length) > MAX_QUERY_LENGTH || '\u0000' in query) {
            throw ExpectedException(
                "검색어는 공백을 제외한 1자 이상, 최대 ${MAX_QUERY_LENGTH}자이며 NUL 문자를 포함할 수 없습니다.",
                HttpStatus.BAD_REQUEST,
            )
        }
        if (reqDto.page < 0 || reqDto.size !in 1..MAX_PAGE_SIZE) {
            throw ExpectedException("페이지 번호는 0 이상, 페이지 크기는 1 이상 $MAX_PAGE_SIZE 이하여야 합니다.", HttpStatus.BAD_REQUEST)
        }

        // 인스턴스별 검색 실행 수를 제한한다. 허가를 기다리는 요청 대기열은 만들지 않는다.
        if (!permits.tryAcquire()) throw unavailable()
        try {
            val total = repository.count(query)
            val items =
                repository.search(query, reqDto.page.toLong() * reqDto.size, reqDto.size).map { document ->
                    ArticleSearchItemResDto(
                        id = document.documentId,
                        title = document.title,
                        snippet = ArticleSearchSnippetBuilder.build(document.searchText, query),
                        updatedAt = document.updatedAt,
                    )
                }
            return ArticleSearchResDto(
                items = items,
                page = reqDto.page,
                size = reqDto.size,
                totalElements = total,
                totalPages = total / reqDto.size + if (total % reqDto.size == 0L) 0 else 1,
            )
        } catch (_: QueryTimeoutException) {
            throw unavailable()
        } catch (_: DataAccessResourceFailureException) {
            throw unavailable()
        } catch (e: UncategorizedMongoDbException) {
            // Spring Data MongoDB는 maxTimeMS 초과를 미분류 예외로 감싼다.
            if (e.cause is MongoExecutionTimeoutException) throw unavailable()
            throw e
        } finally {
            permits.release()
        }
    }

    private fun unavailable() = ExpectedException("문서 검색을 일시적으로 사용할 수 없습니다.", HttpStatus.SERVICE_UNAVAILABLE)
}
