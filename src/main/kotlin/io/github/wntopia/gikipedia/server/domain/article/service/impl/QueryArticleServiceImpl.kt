package io.github.wntopia.gikipedia.server.domain.article.service.impl

import io.github.wntopia.gikipedia.server.domain.article.dto.response.ArticleResDto
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleMongoRepository
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.domain.article.service.ArticleCacheStore
import io.github.wntopia.gikipedia.server.domain.article.service.QueryArticleService
import io.github.wntopia.gikipedia.server.global.config.ArticleReadEnvironment
import io.github.wntopia.gikipedia.server.global.config.ArticleReadSource
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import team.themoment.sdk.exception.ExpectedException

@Service
class QueryArticleServiceImpl(
    private val articleRepository: ArticleRepository,
    private val articleMongoRepository: ArticleMongoRepository,
    private val articleCacheStore: ArticleCacheStore,
    private val readEnvironment: ArticleReadEnvironment,
) : QueryArticleService {
    @Transactional(readOnly = true)
    override fun execute(articleId: Long): ArticleResDto =
        when (readEnvironment.source) {
            ArticleReadSource.MYSQL -> queryFromMysql(articleId)
            ArticleReadSource.REDIS_MONGO -> queryFromCacheOrMongo(articleId)
        }

    private fun queryFromMysql(articleId: Long): ArticleResDto =
        articleRepository
            .findById(articleId)
            .orElseThrow { notFound() }
            .let(ArticleResDto::from)

    /**
     * Redis miss 시 Mongo를 조회해 backfill한다. Mongo까지 miss면(정상적으로는 생성 이벤트가 이미
     * revision=1 baseline을 만들어뒀어야 하므로) MySQL로 조용히 폴백하지 않고 그대로 404를 반환한다 —
     * 그래야 동기화 지연/실패가 가려지지 않고 드러난다.
     */
    private fun queryFromCacheOrMongo(articleId: Long): ArticleResDto {
        articleCacheStore.get(articleId)?.let { return it }

        val fromMongo =
            articleMongoRepository.findByDocumentId(articleId)?.let(ArticleResDto::from)
                ?: throw notFound()
        articleCacheStore.put(fromMongo)
        return fromMongo
    }

    private fun notFound() = ExpectedException("존재하지 않는 게시글입니다.", HttpStatus.NOT_FOUND)
}
