package io.github.wntopia.gikipedia.server.domain.article.service

import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleMongoEntity
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Component

/**
 * MySQL(정본)의 현재 상태를 MongoDB 최신 문서 뷰([ArticleMongoEntity])에 동기화한다.
 *
 * 문서 생성/수정 이벤트 리스너와 일일 정합성 검사 배치가 공유하는 단일 동기화 경로다. 두 단계로 나눠 원자성을 확보한다:
 * 1) `$max`로 revision 필드만 먼저 원자적으로 "선점"한다 — 두 스레드가 동시에 같은 article을 처리해도 항상 더 높은
 *    revision이 최종적으로 남는다.
 * 2) 선점에 성공한(=자신의 revision이 실제로 반영된) 경우에만 MySQL을 재조회해 title/content 등을 반영하되,
 *    이때도 "revision이 그사이 바뀌지 않았을 때만" 조건으로 걸어, 더 최신 이벤트가 끼어들면 자신의 값으로 되돌리지 않는다.
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
        val byDocumentId = Query(Criteria.where("document_id").`is`(articleId))

        val claimed =
            mongoTemplate.findAndModify(
                byDocumentId,
                Update().max("revision", revision),
                FindAndModifyOptions.options().upsert(true).returnNew(true),
                ArticleMongoEntity::class.java,
            )

        if (claimed == null || claimed.revision != revision) {
            // 이미 더 높은(또는 같은) revision이 반영되어 있음 — 이 이벤트는 stale이므로 내용 반영은 건너뛴다.
            return
        }

        val article = articleRepository.findById(articleId).orElse(null)
        if (article == null) {
            log.warn("동기화 대상 article을 MySQL에서 찾을 수 없음 (articleId={})", articleId)
            return
        }

        // document_id + revision을 함께 필터에 걸어, 그사이 더 최신 이벤트가 revision을 앞질렀다면
        // (아래에서 매칭이 안 되어) 조용히 스킵되도록 한다 — 오래된 내용으로 되돌아가는 걸 막는 장치.
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
