package io.github.wntopia.gikipedia.server.domain.article.service

import com.mongodb.client.result.UpdateResult
import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleMongoEntity
import org.assertj.core.api.Assertions.assertThat
import org.bson.BsonObjectId
import org.bson.Document
import org.bson.types.ObjectId
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
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import java.time.Instant

class ArticleSearchSynchronizationTest {
    private val template = mock<MongoTemplate>()
    private val synchronizer = ArticleMongoSynchronizer(template)
    private val content = "== 개요 ==\n[[급식]] 안내"
    private val now = Instant.parse("2026-10-12T00:00:00Z")

    private fun sync() = synchronizer.sync(7, 3, "급식", content, null, now, now)

    @Nested
    @DisplayName("Describe: sync의 검색용 본문 동기화")
    inner class DescribeSearchTextSync {
        @Nested
        @DisplayName("Context: 새 문서를 삽입하면")
        inner class ContextInsert {
            @Test
            @DisplayName("It: 원문과 표시용 본문을 같은 삽입 연산에 넣는다")
            fun includesSearchTextInInsert() {
                // Given
                whenever(template.upsert(any<Query>(), any<Update>(), eq(ArticleMongoEntity::class.java)))
                    .thenReturn(UpdateResult.acknowledged(0, 0L, BsonObjectId(ObjectId())))

                // When
                sync()

                // Then
                val captor = argumentCaptor<Update>()
                verify(template).upsert(any<Query>(), captor.capture(), eq(ArticleMongoEntity::class.java))
                val fields = captor.firstValue.updateObject.get("\$setOnInsert", Document::class.java)
                assertThat(fields["content"]).isEqualTo(content)
                assertThat(fields["search_text"]).isEqualTo("개요 급식 안내")
            }
        }

        @Nested
        @DisplayName("Context: 기존 문서를 갱신하면")
        inner class ContextUpdate {
            @Test
            @DisplayName("It: 같은 리비전 조건으로 원문과 표시용 본문을 함께 갱신한다")
            fun guardsSearchTextUpdateByRevision() {
                // Given
                whenever(template.upsert(any<Query>(), any<Update>(), eq(ArticleMongoEntity::class.java)))
                    .thenReturn(UpdateResult.acknowledged(1, 1L, null))

                // When
                sync()

                // Then
                val query = argumentCaptor<Query>()
                val update = argumentCaptor<Update>()
                verify(template).updateFirst(query.capture(), update.capture(), eq(ArticleMongoEntity::class.java))
                assertThat(query.firstValue.queryObject).containsEntry("document_id", 7L).containsEntry("revision", 3)
                val fields = update.firstValue.updateObject.get("\$set", Document::class.java)
                assertThat(fields["content"]).isEqualTo(content)
                assertThat(fields["search_text"]).isEqualTo("개요 급식 안내")
            }
        }
    }
}
