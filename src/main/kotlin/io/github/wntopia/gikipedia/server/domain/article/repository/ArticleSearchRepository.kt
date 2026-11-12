package io.github.wntopia.gikipedia.server.domain.article.repository

import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleMongoEntity
import io.github.wntopia.gikipedia.server.domain.article.util.ArticleSearchPattern
import io.github.wntopia.gikipedia.server.global.config.ArticleSearchEnvironment
import org.bson.Document
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.AggregationOperation
import org.springframework.data.mongodb.core.aggregation.AggregationOptions
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class ArticleSearchRepository(
    private val mongoTemplate: MongoTemplate,
    private val environment: ArticleSearchEnvironment,
) {
    fun search(
        query: String,
        offset: Long,
        size: Int,
    ): List<ArticleSearchDocument> {
        val pattern = ArticleSearchPattern.literal(query)
        val rank =
            Document(
                "\$switch",
                Document(
                    "branches",
                    listOf(
                        Document("case", titleMatches("\\A$pattern\\z")).append("then", 0),
                        Document("case", titleMatches(pattern)).append("then", 1),
                    ),
                ).append("default", 2),
            )
        val aggregation =
            Aggregation
                .newAggregation(
                    Aggregation.match(criteria(query)),
                    AggregationOperation {
                        Document(
                            "\$project",
                            Document("_id", 0)
                                .append("document_id", 1)
                                .append("title", 1)
                                .append("search_text", 1)
                                .append("article_updated_at", 1),
                        )
                    },
                    AggregationOperation { Document("\$set", Document("search_rank", rank)) },
                    Aggregation.sort(
                        Sort.by(
                            Sort.Order.asc("search_rank"),
                            Sort.Order.desc("article_updated_at"),
                            Sort.Order.desc("document_id"),
                        ),
                    ),
                    Aggregation.skip(offset),
                    Aggregation.limit(size.toLong()),
                    AggregationOperation {
                        Document(
                            "\$project",
                            Document("documentId", "\$document_id")
                                .append("title", 1)
                                .append("searchText", "\$search_text")
                                .append("updatedAt", "\$article_updated_at"),
                        )
                    },
                ).withOptions(AggregationOptions.builder().maxTime(environment.maxTime).build())
        return mongoTemplate.aggregate(aggregation, "articles", ArticleSearchDocument::class.java).mappedResults
    }

    fun count(query: String): Long =
        mongoTemplate.count(
            Query(criteria(query)).maxTime(environment.maxTime),
            ArticleMongoEntity::class.java,
        )

    private fun criteria(query: String): Criteria {
        val pattern = ArticleSearchPattern.literal(query)
        return Criteria().orOperator(
            Criteria.where("title").regex(pattern),
            Criteria.where("search_text").regex(pattern),
        )
    }

    private fun titleMatches(pattern: String) =
        Document(
            "\$regexMatch",
            Document("input", "\$title").append("regex", Document("\$literal", pattern)),
        )
}

data class ArticleSearchDocument(
    val documentId: Long,
    val title: String,
    val searchText: String,
    val updatedAt: Instant,
)
