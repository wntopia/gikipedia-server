package io.github.wntopia.gikipedia.server.domain.article.service

import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleMongoEntity
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Component

/**
 * MySQL(정본)의 현재 상태를 MongoDB 최신 문서 뷰([ArticleMongoEntity])에 동기화한다.
 *
 * 문서 생성/수정 이벤트 리스너와 일일 정합성 검사 배치가 공유하는 단일 동기화 경로다. 원자성은 다음과 같이 확보한다:
 * 1) `document_id` 기준 upsert 하나로 revision을 `$max`로 선점함과 동시에, 신규 삽입인 경우에는
 *    `setOnInsert`로 title/content 등 모든 필드를 함께 채운다 — "revision만 있고 나머지 필드는 없는" 반쪽
 *    문서가 존재하는 순간 자체를 없애, 그 사이 다른 곳(조회, 정합성 배치)이 그 문서를 읽어도 안전하다.
 * 2) 이미 문서가 있던 경우(=삽입이 아니었던 경우)에는 `document_id + revision`을 함께 필터로 건 후속 갱신으로만
 *    content를 반영한다. 그사이 더 최신 이벤트가 revision을 앞질렀다면 필터가 안 맞아 조용히 스킵되므로, 오래된
 *    내용으로 되돌아가는 일이 없다.
 *
 * 이벤트 payload는 articleId/revision만 담고(스키마 안정성) 실제 데이터는 여기서 MySQL을 재조회해 채운다.
 */
@Component
class ArticleMongoSyncService(
    private val articleRepository: ArticleRepository,
    private val mongoTemplate: MongoTemplate,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun sync(
        articleId: Long,
        revision: Int,
    ) {
        val article = articleRepository.findById(articleId).orElse(null)
        if (article == null) {
            log.warn("동기화 대상 article을 MySQL에서 찾을 수 없음 (articleId={})", articleId)
            return
        }

        val byDocumentId = Query(Criteria.where("document_id").`is`(articleId))
        val result =
            mongoTemplate.upsert(
                byDocumentId,
                Update()
                    .max("revision", revision)
                    .setOnInsert("document_id", articleId)
                    .setOnInsert("title", article.title)
                    .setOnInsert("content", article.content)
                    .setOnInsert("image_url", article.imageUrl)
                    .setOnInsert("article_created_at", article.createdAt)
                    .setOnInsert("article_updated_at", article.updatedAt),
                ArticleMongoEntity::class.java,
            )

        if (result.upsertedId != null) {
            // 신규 삽입: setOnInsert로 이미 완전한 문서가 만들어졌으므로 더 할 일이 없다.
            return
        }

        // 기존 문서 갱신: revision이 이번 값으로 실제로 갱신됐을 때만(=자신이 최신일 때만) content를 반영한다.
        mongoTemplate.updateFirst(
            Query(
                Criteria
                    .where("document_id")
                    .`is`(articleId)
                    .and("revision")
                    .`is`(revision),
            ),
            Update()
                .set("title", article.title)
                .set("content", article.content)
                .set("image_url", article.imageUrl)
                .set("article_created_at", article.createdAt)
                .set("article_updated_at", article.updatedAt),
            ArticleMongoEntity::class.java,
        )
    }
}
