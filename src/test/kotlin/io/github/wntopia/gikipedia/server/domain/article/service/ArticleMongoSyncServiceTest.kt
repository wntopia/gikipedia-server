package io.github.wntopia.gikipedia.server.domain.article.service

import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleJpaEntity
import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleMongoEntity
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.test.util.ReflectionTestUtils
import java.time.Instant
import java.util.Optional

/**
 * $max 2단계 원자적 반영 검증: 선점 성공/실패(stale 이벤트)/MySQL 조회 실패 케이스.
 */
class ArticleMongoSyncServiceTest {
    private val articleRepository = mock<ArticleRepository>()
    private val mongoTemplate = mock<MongoTemplate>()
    private val service = ArticleMongoSyncService(articleRepository, mongoTemplate)

    private val article = ArticleJpaEntity(title = "제목", content = "내용")

    @BeforeEach
    fun setUp() {
        ReflectionTestUtils.setField(article, "id", 1L)
    }

    private fun mongoDocWithRevision(revision: Int) =
        ArticleMongoEntity(1L, "old", "old", null, Instant.now(), Instant.now(), revision)

    private fun stubClaim(returnedRevision: Int) {
        whenever(
            mongoTemplate.findAndModify(
                any<Query>(),
                any<Update>(),
                any<FindAndModifyOptions>(),
                eq(ArticleMongoEntity::class.java),
            ),
        ).thenReturn(mongoDocWithRevision(returnedRevision))
    }

    @Test
    fun `선점에 성공하면 MySQL을 재조회해 내용을 반영한다`() {
        stubClaim(returnedRevision = 5)
        whenever(articleRepository.findById(1L)).thenReturn(Optional.of(article))

        service.sync(1L, 5)

        verify(mongoTemplate).updateFirst(any<Query>(), any<Update>(), eq(ArticleMongoEntity::class.java))
    }

    @Test
    fun `이미 더 높은 revision이 반영돼있으면 stale 이벤트이므로 내용 반영을 건너뛴다`() {
        stubClaim(returnedRevision = 6) // 요청한 5보다 이미 높음 — 자신의 요청은 반영되지 않은 것

        service.sync(1L, 5)

        verify(articleRepository, never()).findById(any())
        verify(mongoTemplate, never()).updateFirst(any<Query>(), any<Update>(), eq(ArticleMongoEntity::class.java))
    }

    @Test
    fun `MySQL에서 article을 못 찾으면 내용 반영을 건너뛴다`() {
        stubClaim(returnedRevision = 5)
        whenever(articleRepository.findById(1L)).thenReturn(Optional.empty())

        service.sync(1L, 5)

        verify(mongoTemplate, never()).updateFirst(any<Query>(), any<Update>(), eq(ArticleMongoEntity::class.java))
    }
}
