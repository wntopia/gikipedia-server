package io.github.wntopia.gikipedia.server.domain.article.repository

import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleMongoEntity
import org.springframework.data.mongodb.repository.MongoRepository

interface ArticleMongoRepository : MongoRepository<ArticleMongoEntity, String> {
    fun findByDocumentId(documentId: Long): ArticleMongoEntity?
}
