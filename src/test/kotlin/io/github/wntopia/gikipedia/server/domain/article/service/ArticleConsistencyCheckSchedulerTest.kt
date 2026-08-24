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
import org.springframework.data.domain.Pageable
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

    /** id 청크 1개만 돌려주고 다음 호출에선 빈 목록을 줘서 스캔 루프가 끝나게 하는 스텁. */
    private fun stubSingleChunk(articleIds: List<Long>) {
        whenever(articleRepository.findIdsAfter(eq(0L), any<Pageable>())).thenReturn(articleIds)
        whenever(articleRepository.findIdsAfter(eq(articleIds.last()), any<Pageable>())).thenReturn(emptyList())
    }

    @Test
    fun `mongo가 최신이면 복구하지 않는다`() {
        stubSingleChunk(listOf(1L))
        whenever(
            articleHistoryRepository.findLatestRevisionPerArticleIn(listOf(1L)),
        ).thenReturn(listOf(RevisionView(1L, 3)))
        whenever(articleMongoRepository.findByDocumentIdIn(listOf(1L))).thenReturn(listOf(mongoDoc(1L, 3)))

        scheduler.checkAndRepair()

        verify(articleMongoSynchronizer, never()).sync(any(), any(), any(), any(), any(), any(), any())
    }

    @Test
    fun `mongo가 뒤처져있으면 MySQL에서 다시 조회해 최신 리비전으로 복구한다`() {
        stubSingleChunk(listOf(1L))
        whenever(
            articleHistoryRepository.findLatestRevisionPerArticleIn(listOf(1L)),
        ).thenReturn(listOf(RevisionView(1L, 5)))
        whenever(articleMongoRepository.findByDocumentIdIn(listOf(1L))).thenReturn(listOf(mongoDoc(1L, 3)))

        scheduler.checkAndRepair()

        verify(articleMongoSynchronizer).sync(eq(1L), eq(5), eq("제목"), eq("내용"), eq(null), any(), any())
    }

    @Test
    fun `mongo에 문서 자체가 없으면 복구한다`() {
        stubSingleChunk(listOf(1L))
        whenever(articleHistoryRepository.findLatestRevisionPerArticleIn(listOf(1L))).thenReturn(emptyList())
        whenever(articleMongoRepository.findByDocumentIdIn(listOf(1L))).thenReturn(emptyList())

        scheduler.checkAndRepair()

        // 히스토리가 없는(=한 번도 수정 안 된) 문서의 정답 리비전은 1이다.
        verify(articleMongoSynchronizer).sync(eq(1L), eq(1), any(), any(), anyOrNull(), any(), any())
    }

    @Test
    fun `복구 대상 article을 MySQL에서 못 찾으면 건너뛰고 계속 진행한다`() {
        stubSingleChunk(listOf(1L))
        whenever(
            articleHistoryRepository.findLatestRevisionPerArticleIn(listOf(1L)),
        ).thenReturn(listOf(RevisionView(1L, 5)))
        whenever(articleMongoRepository.findByDocumentIdIn(listOf(1L))).thenReturn(listOf(mongoDoc(1L, 3)))
        whenever(articleRepository.findById(1L)).thenReturn(Optional.empty())

        scheduler.checkAndRepair()

        verify(articleMongoSynchronizer, never()).sync(any(), any(), any(), any(), any(), any(), any())
    }

    @Test
    fun `article id를 청크 단위로 나눠 스캔하고 청크에 속한 리비전만 조회한다`() {
        whenever(articleRepository.findIdsAfter(eq(0L), any<Pageable>())).thenReturn(listOf(1L))
        whenever(articleRepository.findIdsAfter(eq(1L), any<Pageable>())).thenReturn(listOf(2L))
        whenever(articleRepository.findIdsAfter(eq(2L), any<Pageable>())).thenReturn(emptyList())
        whenever(articleHistoryRepository.findLatestRevisionPerArticleIn(any())).thenReturn(emptyList())
        whenever(articleMongoRepository.findByDocumentIdIn(any())).thenReturn(emptyList())
        whenever(articleRepository.findById(2L)).thenReturn(Optional.empty())

        scheduler.checkAndRepair()

        // 전체 조회가 아니라 청크별로 나눠 조회해야 상주 메모리가 문서 수와 무관하게 유지된다.
        verify(articleHistoryRepository).findLatestRevisionPerArticleIn(listOf(1L))
        verify(articleHistoryRepository).findLatestRevisionPerArticleIn(listOf(2L))
        verify(articleMongoRepository).findByDocumentIdIn(listOf(1L))
        verify(articleMongoRepository).findByDocumentIdIn(listOf(2L))
    }
}
