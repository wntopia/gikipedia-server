package io.github.wntopia.gikipedia.server.domain.article.repository

import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleMongoEntity
import org.springframework.data.mongodb.repository.MongoRepository

interface ArticleMongoRepository : MongoRepository<ArticleMongoEntity, String> {
    fun findByDocumentId(documentId: Long): ArticleMongoEntity?

    /** 일일 정합성 검사 배치용 — 주어진 article 청크에 해당하는 복제본만 조회한다. */
    fun findByDocumentIdIn(documentIds: Collection<Long>): List<ArticleMongoEntity>
}
