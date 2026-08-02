package io.github.wntopia.gikipedia.server.domain.article.entity

import io.github.wntopia.gikipedia.server.global.entity.BaseMongoEntity
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import java.time.Instant

/**
 * 문서의 최신 리비전만 담는 읽기 전용 뷰. 과거 리비전은 RDB의 스냅샷 + diff로 재구성하며 여기 저장하지 않는다.
 *
 * RDB가 진실의 원천이고 이 뷰는 파생물이라, 문서 생성/수정 트랜잭션 커밋 이후 이벤트로 동기화된다(eventual consistency).
 * `createdAt`/`updatedAt`([BaseMongoEntity])은 이 Mongo 문서 자체의 감사 필드(동기화된 시각)이므로, 원본 article의
 * 생성/수정 시각은 별도 필드(`articleCreatedAt`/`articleUpdatedAt`)로 들고 있어야 읽기 응답을 정확히 재구성할 수 있다.
 */
@Document(collection = "articles")
class ArticleMongoEntity(
    @field:Indexed(unique = true) @field:Field("document_id") val documentId: Long,
    @field:Field("title") val title: String,
    @field:Field("content") val content: String,
    @field:Field("image_url") val imageUrl: String?,
    @field:Field("article_created_at") val articleCreatedAt: Instant,
    @field:Field("article_updated_at") val articleUpdatedAt: Instant,
    @field:Field("revision") val revision: Int,
) : BaseMongoEntity()
