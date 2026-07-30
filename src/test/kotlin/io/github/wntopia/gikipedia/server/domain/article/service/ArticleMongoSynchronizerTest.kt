package io.github.wntopia.gikipedia.server.domain.article.service

import com.mongodb.client.result.UpdateResult
import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleMongoEntity
import org.bson.BsonObjectId
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import java.time.Instant

/**
 * upsert-then-guarded-update 방식 검증: 신규 삽입은 setOnInsert만으로 끝나고(추가 갱신 없음),
 * 기존 문서 갱신은 후속 updateFirst로 이어진다. 반영할 데이터는 모두 호출자가 넘기므로 MySQL을
 * 재조회하지 않는다.
 */
class ArticleMongoSynchronizerTest {
    private val mongoTemplate = mock<MongoTemplate>()
    private val service = ArticleMongoSynchronizer(mongoTemplate)

    private val now = Instant.now()

    private fun updateResult(upserted: Boolean): UpdateResult =
        UpdateResult.acknowledged(1, 1L, if (upserted) BsonObjectId(ObjectId()) else null)

    private fun sync(revision: Int) {
        service.sync(
            articleId = 1L,
            revision = revision,
            title = "제목",
            content = "내용",
            imageUrl = null,
            articleCreatedAt = now,
            articleUpdatedAt = now,
        )
    }

    @Test
    fun `신규 문서 삽입이면 setOnInsert만으로 끝나고 후속 갱신을 호출하지 않는다`() {
        whenever(
            mongoTemplate.upsert(any<Query>(), any<Update>(), eq(ArticleMongoEntity::class.java)),
        ).thenReturn(updateResult(upserted = true))

        sync(revision = 1)

        verify(mongoTemplate, never()).updateFirst(any<Query>(), any<Update>(), eq(ArticleMongoEntity::class.java))
    }

    @Test
    fun `기존 문서 갱신이면 document_id+revision 필터로 후속 갱신을 호출한다`() {
        whenever(
            mongoTemplate.upsert(any<Query>(), any<Update>(), eq(ArticleMongoEntity::class.java)),
        ).thenReturn(updateResult(upserted = false))

        sync(revision = 5)

        verify(mongoTemplate).updateFirst(any<Query>(), any<Update>(), eq(ArticleMongoEntity::class.java))
    }

    @Test
    fun `동시 upsert 경합으로 DuplicateKeyException이 나면 한 번 재시도해 갱신 경로를 탄다`() {
        whenever(
            mongoTemplate.upsert(any<Query>(), any<Update>(), eq(ArticleMongoEntity::class.java)),
        ).thenThrow(DuplicateKeyException("E11000 duplicate key"))
            .thenReturn(updateResult(upserted = false))

        sync(revision = 1)

        verify(mongoTemplate, times(2)).upsert(any<Query>(), any<Update>(), eq(ArticleMongoEntity::class.java))
        verify(mongoTemplate).updateFirst(any<Query>(), any<Update>(), eq(ArticleMongoEntity::class.java))
    }
}
