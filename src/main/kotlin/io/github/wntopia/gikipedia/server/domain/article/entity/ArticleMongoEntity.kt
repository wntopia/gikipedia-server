package io.github.wntopia.gikipedia.server.domain.article.entity

import io.github.wntopia.gikipedia.server.global.entity.BaseMongoEntity
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = "articles")
class ArticleMongoEntity(
    @Field("document_id") val documentId: Long,
    @Field("title") val title: String,
    @Field("content") val content: String,
) : BaseMongoEntity()
