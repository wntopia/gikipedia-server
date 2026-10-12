package io.github.wntopia.gikipedia.server.domain.article.repository

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.event.CommandListener
import com.mongodb.event.CommandStartedEvent
import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleMongoEntity
import io.github.wntopia.gikipedia.server.domain.article.service.ArticleMongoSynchronizer
import io.github.wntopia.gikipedia.server.domain.article.service.ArticleSearchPattern
import io.github.wntopia.gikipedia.server.domain.article.service.ArticleSearchSnippetBuilder
import io.github.wntopia.gikipedia.server.global.config.ArticleSearchEnvironment
import org.assertj.core.api.Assertions.assertThat
import org.bson.BsonDocument
import org.bson.Document
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.mongodb.MongoDBContainer
import java.time.Instant
import java.util.Date
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.system.measureNanoTime

@Testcontainers
class ArticleSearchRepositoryIntegrationTest {
    companion object {
        @Container
        @JvmField
        val mongo = MongoDBContainer("mongo:7")
    }

    private lateinit var client: MongoClient
    private lateinit var template: MongoTemplate
    private lateinit var repository: ArticleSearchRepository
    private lateinit var synchronizer: ArticleMongoSynchronizer
    private val commands = CopyOnWriteArrayList<BsonDocument>()
    private val now = Instant.parse("2026-10-12T00:00:00Z")

    @BeforeEach
    fun setUp() {
        client =
            MongoClients.create(
                MongoClientSettings
                    .builder()
                    .applyConnectionString(ConnectionString(mongo.connectionString))
                    .addCommandListener(
                        object : CommandListener {
                            override fun commandStarted(event: CommandStartedEvent) {
                                if (event.commandName == "aggregate") {
                                    commands.add(BsonDocument.parse(event.command.toJson()))
                                }
                            }
                        },
                    ).build(),
            )
        template = MongoTemplate(client, "article_search_test")
        template.dropCollection(ArticleMongoEntity::class.java)
        template
            .indexOps(ArticleMongoEntity::class.java)
            .ensureIndex(Index().on("document_id", Sort.Direction.ASC).unique())
        repository = ArticleSearchRepository(template, ArticleSearchEnvironment())
        synchronizer = ArticleMongoSynchronizer(template)
        commands.clear()
    }

    @AfterEach
    fun tearDown() {
        client.close()
    }

    private fun article(
        id: Long,
        title: String,
        content: String,
        updated: Instant = now,
        revision: Int = 1,
    ) {
        synchronizer.sync(id, revision, title, content, null, now.minusSeconds(3600), updated)
    }

    @Test
    fun `표시용 본문과 제목을 검색하고 전체 순위로 페이지를 나눈다`() {
        article(1, "급식", "제목만 일치", now.minusSeconds(100))
        article(2, "급식 안내", "[[급식]] 안내", now.minusSeconds(10))
        article(3, "학교", "== 안내 ==\n오늘의 [[급식]]", now.plusSeconds(100))
        article(4, "급식", "본문", now.minusSeconds(100))
        article(5, "무관", "무관")

        assertThat(repository.count("급식")).isEqualTo(4)
        assertThat(repository.search("급식", 0, 2).map { it.documentId }).containsExactly(4, 1)
        assertThat(repository.search("급식", 2, 2).map { it.documentId }).containsExactly(2, 3)
        assertThat(repository.search("급식", 4, 2)).isEmpty()
        assertThat(repository.search("급식", Int.MAX_VALUE.toLong() * 50, 50)).isEmpty()
        assertThat(repository.search("급식", 0, 50).first().updatedAt).isEqualTo(now.minusSeconds(100))
        assertThat(repository.search("급식 안내", 0, 20).single().searchText).isEqualTo("급식 안내")
        assertThat(repository.count("급식  안내")).isZero()
        assertThat(repository.count("없음")).isZero()
    }

    @ParameterizedTest
    @ValueSource(strings = [".*", "[", "]", "\\E", "\$title", "(x)", "'\"", "🍱", "급식 안내"])
    fun `특수문자와 Unicode를 Mongo와 JVM에서 같은 리터럴로 찾는다`(query: String) {
        article(1, query, "앞 $query 뒤")
        article(2, "다른 문서", "아무 내용")
        val document = repository.search(query, 0, 20).single()
        assertThat(document.documentId).isEqualTo(1)
        assertThat(repository.count(query)).isEqualTo(1)
        assertThat(
            ArticleSearchSnippetBuilder
                .build(document.searchText, query)
                .filter { it.highlight }
                .single()
                .text,
        ).isEqualTo(query)
    }

    @Test
    fun `영문 대소문자와 정확히 일치의 끝 경계를 검증한다`() {
        article(1, "GSM", "gsm", now.minusSeconds(100))
        article(2, "gsm\n", "본문", now.plusSeconds(100))
        article(3, "GSM 안내", "본문", now)
        assertThat(repository.search("gsm", 0, 20).map { it.documentId }).containsExactly(1, 2, 3)

        article(4, "İ ı ſ K", "본문")
        assertThat(repository.count("i")).isZero()
        assertThat(repository.count("k")).isZero()
        assertThat(repository.count("İ")).isEqualTo(1)
    }

    @Test
    fun `수정은 검색 본문을 교체하고 오래된 이벤트는 최신 본문을 되돌리지 않는다`() {
        article(1, "학교", "[[이전]] 본문")
        article(1, "학교", "== 최신 ==\n[[급식]] 안내", now.plusSeconds(10), 3)
        article(1, "학교", "잘못된 [[과거]]", now.plusSeconds(5), 2)
        assertThat(repository.count("이전")).isZero()
        assertThat(repository.count("과거")).isZero()
        assertThat(repository.count("급식 안내")).isEqualTo(1)
        val stored =
            requireNotNull(
                template.findOne(Query(Criteria.where("document_id").`is`(1)), ArticleMongoEntity::class.java),
            )
        assertThat(stored.id).isNotBlank()
        assertThat(stored.searchText).isEqualTo("최신 급식 안내")
        assertThat(stored.content).isEqualTo("== 최신 ==\n[[급식]] 안내")
        assertThat(stored.revision).isEqualTo(3)
        assertThat(stored.articleUpdatedAt).isEqualTo(now.plusSeconds(10))
    }

    @Test
    fun `목록과 전체 건수 DB 명령 모두 2초 제한을 적용한다`() {
        article(1, "급식", "본문")
        repository.count("급식")
        repository.search("급식", 0, 20)
        assertThat(commands).hasSize(2)
        assertThat(commands.map { it.getNumber("maxTimeMS").longValue() }).containsExactly(2000, 2000)
    }

    @Test
    fun `합성 데이터에서 짧은 검색어와 깊은 페이지의 실행 비용을 기록한다`() {
        val body = "학교 생활 안내 ".repeat(200) + "급식 안내"
        template.getCollection("articles").insertMany(
            (1L..2000L).map { id ->
                Document("document_id", id)
                    .append("title", "문서 $id")
                    .append("content", body)
                    .append("search_text", body)
                    .append("article_created_at", Date.from(now))
                    .append("article_updated_at", Date.from(now))
                    .append("revision", 1)
            },
        )
        val version = template.executeCommand(Document("buildInfo", 1)).getString("version")
        for ((query, offset) in listOf("학" to 0L, "급식" to 0L, "없는문서" to 0L, "급식" to 1980L)) {
            val times =
                (0..5)
                    .map {
                        measureNanoTime {
                            repository.count(query)
                            repository.search(query, offset, 20)
                        } / 1_000_000.0
                    }.drop(1)
                    .sorted()
            println(
                "SEARCH_BENCHMARK mongo=$version docs=2000 bodyCodePoints=${body.length} " +
                    "query=$query offset=$offset samples=5 medianMs=${times[2]} maxMs=${times.last()}",
            )
        }
        val filter =
            Document(
                "\$or",
                listOf(
                    Document("title", Document("\$regex", ArticleSearchPattern.literal("급식"))),
                    Document("search_text", Document("\$regex", ArticleSearchPattern.literal("급식"))),
                ),
            )
        val stats =
            template
                .executeCommand(
                    Document("explain", Document("find", "articles").append("filter", filter).append("maxTimeMS", 2000))
                        .append("verbosity", "executionStats"),
                ).get("executionStats", Document::class.java)
        println(
            "SEARCH_EXPLAIN docsExamined=${stats["totalDocsExamined"]} " +
                "keysExamined=${stats["totalKeysExamined"]} returned=${stats["nReturned"]}",
        )
        assertThat(repository.count("급식")).isEqualTo(2000)
    }
}
