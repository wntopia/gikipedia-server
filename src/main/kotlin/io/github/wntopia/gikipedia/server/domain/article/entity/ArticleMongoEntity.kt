package io.github.wntopia.gikipedia.server.domain.article.entity

import io.github.wntopia.gikipedia.server.global.entity.BaseMongoEntity
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

/**
 * 문서의 최신 리비전만 담는 읽기 전용 뷰. 과거 리비전은 RDB의 스냅샷 + diff로 재구성하며 여기 저장하지 않는다.
 *
 * RDB가 진실의 원천이고 이 뷰는 파생물이라, 문서 수정 트랜잭션 커밋 이후 이벤트로 동기화된다(eventual consistency).
 */
@Document(collection = "articles")
class ArticleMongoEntity(
    @Field("document_id") val documentId: Long,
    @Field("title") val title: String,
    @Field("content") val content: String,
    @Field("revision") val revision: Int,
) : BaseMongoEntity()
