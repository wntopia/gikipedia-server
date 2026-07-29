package io.github.wntopia.gikipedia.server.domain.article.service

import com.mongodb.client.result.UpdateResult
import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleJpaEntity
import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleMongoEntity
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import org.bson.BsonObjectId
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.test.util.ReflectionTestUtils
import java.util.Optional

/**
 * upsert-then-guarded-update 방식 검증: 신규 삽입은 setOnInsert만으로 끝나고(추가 갱신 없음),
 * 기존 문서 갱신은 후속 updateFirst로 이어지며, MySQL에 없는 article은 Mongo를 아예 건드리지 않는다.
 */
class ArticleMongoSyncServiceTest {
    private val articleRepository = mock<ArticleRepository>()
    private val mongoTemplate = mock<MongoTemplate>()
    private val service = ArticleMongoSyncService(articleRepository, mongoTemplate)

    private val article = ArticleJpaEntity(title = "제목", content = "내용")

    @BeforeEach
    fun setUp() {
        ReflectionTestUtils.setField(article, "id", 1L)
        whenever(articleRepository.findById(1L)).thenReturn(Optional.of(article))
    }

    private fun updateResult(upserted: Boolean): UpdateResult =
        UpdateResult.acknowledged(1, 1L, if (upserted) BsonObjectId(ObjectId()) else null)

    @Test
    fun `신규 문서 삽입이면 setOnInsert만으로 끝나고 후속 갱신을 호출하지 않는다`() {
        whenever(
            mongoTemplate.upsert(any<Query>(), any<Update>(), eq(ArticleMongoEntity::class.java)),
        ).thenReturn(updateResult(upserted = true))

        service.sync(1L, 1)

        verify(mongoTemplate, never()).updateFirst(any<Query>(), any<Update>(), eq(ArticleMongoEntity::class.java))
    }

    @Test
    fun `기존 문서 갱신이면 document_id+revision 필터로 후속 갱신을 호출한다`() {
        whenever(
            mongoTemplate.upsert(any<Query>(), any<Update>(), eq(ArticleMongoEntity::class.java)),
        ).thenReturn(updateResult(upserted = false))

        service.sync(1L, 5)

        verify(mongoTemplate).updateFirst(any<Query>(), any<Update>(), eq(ArticleMongoEntity::class.java))
    }

    @Test
    fun `MySQL에서 article을 못 찾으면 Mongo를 전혀 건드리지 않는다`() {
        whenever(articleRepository.findById(1L)).thenReturn(Optional.empty())

        service.sync(1L, 5)

        verify(mongoTemplate, never()).upsert(any<Query>(), any<Update>(), eq(ArticleMongoEntity::class.java))
        verify(mongoTemplate, never()).updateFirst(any<Query>(), any<Update>(), eq(ArticleMongoEntity::class.java))
    }
}
