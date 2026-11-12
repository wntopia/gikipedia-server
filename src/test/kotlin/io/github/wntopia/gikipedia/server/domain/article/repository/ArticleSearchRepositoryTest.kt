package io.github.wntopia.gikipedia.server.domain.article.repository

import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleMongoEntity
import io.github.wntopia.gikipedia.server.global.config.ArticleSearchEnvironment
import org.assertj.core.api.Assertions.assertThat
import org.bson.Document
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.AggregationResults
import org.springframework.data.mongodb.core.query.Query
import java.time.Duration
import java.time.Instant
import java.util.regex.Pattern

class ArticleSearchRepositoryTest {
    private val template = mock<MongoTemplate>()
    private val repository =
        ArticleSearchRepository(template, ArticleSearchEnvironment(maxTime = Duration.ofMillis(500)))

    @Nested
    @DisplayName("Describe: count")
    inner class DescribeCount {
        @Nested
        @DisplayName("Context: 정규식 특수문자를 포함한 검색어를 전달하면")
        inner class ContextLiteralQuery {
            private lateinit var query: Query
            private var result = 0L

            @BeforeEach
            fun setUp() {
                // Given
                whenever(template.count(any<Query>(), eq(ArticleMongoEntity::class.java))).thenReturn(23)

                // When
                result = repository.count("GSM.*")
                val captor = argumentCaptor<Query>()
                verify(template).count(captor.capture(), eq(ArticleMongoEntity::class.java))
                query = captor.firstValue
            }

            @Test
            @DisplayName("It: 제목 또는 표시용 본문에 리터럴 포함 조건을 전달한다")
            fun buildsLiteralFilter() {
                // Then
                val alternatives = query.queryObject.getList("\$or", Document::class.java)
                assertThat(alternatives).hasSize(2)
                assertThat((alternatives[0]["title"] as Pattern).pattern()).isEqualTo("[gG][sS][mM]\\.\\*")
                assertThat((alternatives[1]["search_text"] as Pattern).pattern()).isEqualTo("[gG][sS][mM]\\.\\*")
            }

            @Test
            @DisplayName("It: 설정한 명령 제한 시간을 전달한다")
            fun appliesTimeout() {
                // Then
                assertThat(query.meta.maxTimeMsec).isEqualTo(500)
            }

            @Test
            @DisplayName("It: 저장소가 반환한 전체 건수를 유지한다")
            fun returnsCount() {
                // Then
                assertThat(result).isEqualTo(23)
            }
        }
    }

    @Nested
    @DisplayName("Describe: search")
    inner class DescribeSearch {
        @Nested
        @DisplayName("Context: 달러로 시작하는 검색어와 페이지 범위를 전달하면")
        inner class ContextRankedPage {
            private val document = ArticleSearchDocument(7, "제목", "본문", Instant.parse("2026-10-12T00:00:00Z"))
            private lateinit var aggregation: Aggregation
            private lateinit var pipeline: List<Document>
            private lateinit var result: List<ArticleSearchDocument>

            @BeforeEach
            fun setUp() {
                // Given
                whenever(template.aggregate(any<Aggregation>(), eq("articles"), eq(ArticleSearchDocument::class.java)))
                    .thenReturn(AggregationResults(listOf(document), Document()))

                // When
                result = repository.search("\$title", 100, 10)
                val captor = argumentCaptor<Aggregation>()
                verify(template).aggregate(captor.capture(), eq("articles"), eq(ArticleSearchDocument::class.java))
                aggregation = captor.firstValue
                pipeline = aggregation.toPipeline(Aggregation.DEFAULT_CONTEXT)
            }

            @Test
            @DisplayName("It: 제목과 표시용 본문에 같은 리터럴 필터를 전달한다")
            fun buildsLiteralFilter() {
                // Then
                val match = pipeline.first().get("\$match", Document::class.java)
                val alternatives = match.getList("\$or", Document::class.java)
                val expected = "\\\$[tT][iI][tT][lL][eE]"
                assertThat((alternatives[0]["title"] as Pattern).pattern()).isEqualTo(expected)
                assertThat((alternatives[1]["search_text"] as Pattern).pattern()).isEqualTo(expected)
            }

            @Test
            @DisplayName("It: 제목 정확 일치와 부분 일치를 우선하고 검색어를 집계 표현식으로 해석하지 않는다")
            fun buildsLiteralRankConditions() {
                // Then
                val set = pipeline.single { it.containsKey("\$set") }.get("\$set", Document::class.java)
                val rank = set.get("search_rank", Document::class.java).get("\$switch", Document::class.java)
                val branches = rank.getList("branches", Document::class.java)
                assertThat(branches.map { it["then"] }).containsExactly(0, 1)
                assertThat(rank["default"]).isEqualTo(2)
                val patterns =
                    branches.map {
                        it
                            .get("case", Document::class.java)
                            .get("\$regexMatch", Document::class.java)
                            .get("regex", Document::class.java)
                            .getString("\$literal")
                    }
                assertThat(patterns).containsExactly("\\A\\\$[tT][iI][tT][lL][eE]\\z", "\\\$[tT][iI][tT][lL][eE]")
            }

            @Test
            @DisplayName("It: 순위와 수정 시각과 ID로 정렬한 뒤 페이지를 자른다")
            fun sortsBeforePagination() {
                // Then
                val sortIndex = pipeline.indexOfFirst { it.containsKey("\$sort") }
                val skipIndex = pipeline.indexOfFirst { it.containsKey("\$skip") }
                val limitIndex = pipeline.indexOfFirst { it.containsKey("\$limit") }
                val sort = pipeline[sortIndex].get("\$sort", Document::class.java)
                assertThat(sort.keys).containsExactly("search_rank", "article_updated_at", "document_id")
                assertThat(sort.values).containsExactly(1, -1, -1)
                assertThat(sortIndex).isLessThan(skipIndex)
                assertThat(skipIndex).isLessThan(limitIndex)
                assertThat(pipeline[skipIndex]["\$skip"]).isEqualTo(100L)
                assertThat(pipeline[limitIndex]["\$limit"]).isEqualTo(10L)
            }

            @Test
            @DisplayName("It: 설정한 명령 제한 시간을 집계 옵션에 전달한다")
            fun appliesTimeout() {
                // Then
                assertThat(aggregation.options.maxTime).isEqualTo(Duration.ofMillis(500))
            }

            @Test
            @DisplayName("It: 저장소가 반환한 문서와 순서를 유지한다")
            fun returnsMappedDocuments() {
                // Then
                assertThat(result).containsExactly(document)
            }
        }
    }
}
