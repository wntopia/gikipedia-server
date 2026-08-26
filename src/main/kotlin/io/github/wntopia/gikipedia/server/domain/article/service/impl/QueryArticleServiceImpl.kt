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
import team.themoment.sdk.exception.ExpectedException

@Service
class QueryArticleServiceImpl(
    private val articleRepository: ArticleRepository,
    private val articleMongoRepository: ArticleMongoRepository,
    private val articleCacheStore: ArticleCacheStore,
    private val readEnvironment: ArticleReadEnvironment,
) : QueryArticleService {
    override fun execute(articleId: Long): ArticleResDto =
        when (readEnvironment.source) {
            ArticleReadSource.MYSQL -> queryFromMysql(articleId)
            ArticleReadSource.REDIS_MONGO -> queryFromCacheOrMongo(articleId)
        }

    // REDIS_MONGO 경로는 MySQL을 전혀 안 건드리므로 여기 클래스/메서드 레벨로 @Transactional을 걸어두면
    // 그 경우에도 매번 불필요하게 MySQL 커넥션을 열게 된다. articleRepository.findById()는 Spring Data
    // JPA의 SimpleJpaRepository가 이미 자체 @Transactional(readOnly = true)을 갖고 있어서, 이 메서드
    // 하나만 실행될 때 그 호출에 한해서만 짧게 트랜잭션이 열리고 닫힌다.
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
