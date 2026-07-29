package io.github.wntopia.gikipedia.server.domain.article.service

import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleMongoEntity
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * 전달받은 문서 데이터를 MongoDB 최신 문서 뷰([ArticleMongoEntity])에 동기화한다.
 *
 * 호출 측(이벤트 리스너, 일일 정합성 검사 배치)이 이미 반영할 데이터를 들고 있다고 가정하고, 이 클래스는
 * MySQL을 다시 조회하지 않는다. 문서 생성/수정 이벤트 리스너와 일일 정합성 검사 배치가 공유하는 단일
 * 동기화 경로다. 원자성은 다음과 같이 확보한다:
 * 1) `document_id` 기준 upsert 하나로 revision을 `$max`로 선점함과 동시에, 신규 삽입인 경우에는
 *    `setOnInsert`로 title/content 등 모든 필드를 함께 채운다 — "revision만 있고 나머지 필드는 없는" 반쪽
 *    문서가 존재하는 순간 자체를 없애, 그 사이 다른 곳(조회, 정합성 배치)이 그 문서를 읽어도 안전하다.
 * 2) 이미 문서가 있던 경우(=삽입이 아니었던 경우)에는 `document_id + revision`을 함께 필터로 건 후속 갱신으로만
 *    content를 반영한다. 그사이 더 최신 이벤트가 revision을 앞질렀다면 필터가 안 맞아 조용히 스킵되므로, 오래된
 *    내용으로 되돌아가는 일이 없다.
 */
@Component
class ArticleMongoSynchronizer(
    private val mongoTemplate: MongoTemplate,
) {
    fun sync(
        articleId: Long,
        revision: Int,
        title: String,
        content: String,
        imageUrl: String?,
        articleCreatedAt: Instant,
        articleUpdatedAt: Instant,
    ) {
        val byDocumentId = Query(Criteria.where("document_id").`is`(articleId))
        val result =
            mongoTemplate.upsert(
                byDocumentId,
                Update()
                    .max("revision", revision)
                    .setOnInsert("document_id", articleId)
                    .setOnInsert("title", title)
                    .setOnInsert("content", content)
                    .setOnInsert("image_url", imageUrl)
                    .setOnInsert("article_created_at", articleCreatedAt)
                    .setOnInsert("article_updated_at", articleUpdatedAt),
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
                .set("title", title)
                .set("content", content)
                .set("image_url", imageUrl)
                .set("article_created_at", articleCreatedAt)
                .set("article_updated_at", articleUpdatedAt),
            ArticleMongoEntity::class.java,
        )
    }
}
