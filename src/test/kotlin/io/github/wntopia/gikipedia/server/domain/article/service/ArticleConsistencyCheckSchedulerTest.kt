package io.github.wntopia.gikipedia.server.domain.article.service

import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleJpaEntity
import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleMongoEntity
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleMongoRepository
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.domain.history.repository.ArticleHistoryRepository
import io.github.wntopia.gikipedia.server.domain.history.repository.ArticleLatestRevisionView
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.test.util.ReflectionTestUtils
import java.time.Instant
import java.util.Optional

private data class RevisionView(
    private val articleId: Long,
    private val revision: Int,
) : ArticleLatestRevisionView {
    override fun getArticleId() = articleId

    override fun getRevision() = revision
}

/** MySQL↔Mongo 리비전 불일치 탐지/복구 로직 검증. */
class ArticleConsistencyCheckSchedulerTest {
    private val articleRepository = mock<ArticleRepository>()
    private val articleHistoryRepository = mock<ArticleHistoryRepository>()
    private val articleMongoRepository = mock<ArticleMongoRepository>()
    private val articleMongoSynchronizer = mock<ArticleMongoSynchronizer>()

    private val scheduler =
        ArticleConsistencyCheckScheduler(
            articleRepository,
            articleHistoryRepository,
            articleMongoRepository,
            articleMongoSynchronizer,
        )

    private val article = ArticleJpaEntity(title = "제목", content = "내용")

    @BeforeEach
    fun setUp() {
        ReflectionTestUtils.setField(article, "id", 1L)
        ReflectionTestUtils.setField(article, "createdAt", Instant.now())
        ReflectionTestUtils.setField(article, "updatedAt", Instant.now())
        whenever(articleRepository.findById(1L)).thenReturn(Optional.of(article))
    }

    private fun mongoDoc(
        documentId: Long,
        revision: Int,
    ) = ArticleMongoEntity(documentId, "t", "c", null, Instant.now(), Instant.now(), revision)

    @Test
    fun `mongo가 최신이면 복구하지 않는다`() {
        whenever(articleRepository.findAllIds()).thenReturn(listOf(1L))
        whenever(articleHistoryRepository.findLatestRevisionPerArticle()).thenReturn(listOf(RevisionView(1L, 3)))
        whenever(articleMongoRepository.findAll()).thenReturn(listOf(mongoDoc(1L, 3)))

        scheduler.checkAndRepair()

        verify(articleMongoSynchronizer, never()).sync(any(), any(), any(), any(), any(), any(), any())
    }

    @Test
    fun `mongo가 뒤처져있으면 MySQL에서 다시 조회해 최신 리비전으로 복구한다`() {
        whenever(articleRepository.findAllIds()).thenReturn(listOf(1L))
        whenever(articleHistoryRepository.findLatestRevisionPerArticle()).thenReturn(listOf(RevisionView(1L, 5)))
        whenever(articleMongoRepository.findAll()).thenReturn(listOf(mongoDoc(1L, 3)))

        scheduler.checkAndRepair()

        verify(articleMongoSynchronizer).sync(eq(1L), eq(5), eq("제목"), eq("내용"), eq(null), any(), any())
    }

    @Test
    fun `mongo에 문서 자체가 없으면 복구한다`() {
        whenever(articleRepository.findAllIds()).thenReturn(listOf(1L))
        whenever(articleHistoryRepository.findLatestRevisionPerArticle()).thenReturn(emptyList())
        whenever(articleMongoRepository.findAll()).thenReturn(emptyList())

        scheduler.checkAndRepair()

        // 히스토리가 없는(=한 번도 수정 안 된) 문서의 정답 리비전은 1이다.
        verify(articleMongoSynchronizer).sync(eq(1L), eq(1), any(), any(), anyOrNull(), any(), any())
    }

    @Test
    fun `복구 대상 article을 MySQL에서 못 찾으면 건너뛰고 계속 진행한다`() {
        whenever(articleRepository.findAllIds()).thenReturn(listOf(1L))
        whenever(articleHistoryRepository.findLatestRevisionPerArticle()).thenReturn(listOf(RevisionView(1L, 5)))
        whenever(articleMongoRepository.findAll()).thenReturn(listOf(mongoDoc(1L, 3)))
        whenever(articleRepository.findById(1L)).thenReturn(Optional.empty())

        scheduler.checkAndRepair()

        verify(articleMongoSynchronizer, never()).sync(any(), any(), any(), any(), any(), any(), any())
    }
}
