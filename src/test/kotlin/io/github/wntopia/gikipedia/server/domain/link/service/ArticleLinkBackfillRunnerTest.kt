package io.github.wntopia.gikipedia.server.domain.link.service

import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleJpaEntity
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.domain.link.repository.ArticleLinkRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.Pageable
import java.util.Optional

class ArticleLinkBackfillRunnerTest {
    private val articleRepository = mock<ArticleRepository>()
    private val articleLinkRepository = mock<ArticleLinkRepository>()
    private val indexer = mock<ArticleLinkIndexer>()
    private val runner = ArticleLinkBackfillRunner(articleRepository, articleLinkRepository, indexer)

    private fun stubArticles(vararg ids: Long) {
        whenever(articleRepository.findIdsAfter(eq(0L), any<Pageable>())).thenReturn(ids.toList())
        whenever(articleRepository.findIdsAfter(eq(ids.last()), any<Pageable>())).thenReturn(emptyList())
        ids.forEach {
            whenever(
                articleRepository.findById(it),
            ).thenReturn(Optional.of(ArticleJpaEntity("문서$it", "[[링크]]")))
        }
    }

    @Test
    @DisplayName("링크 테이블이 비어 있으면 모든 문서를 색인한다")
    fun backfillsWhenEmpty() {
        whenever(articleLinkRepository.count()).thenReturn(0)
        stubArticles(1L, 2L)

        runner.backfillIfEmpty()

        verify(indexer).reindex(1L, "문서1", "[[링크]]")
        verify(indexer).reindex(2L, "문서2", "[[링크]]")
    }

    @Test
    @DisplayName("이미 링크가 있으면 아무것도 하지 않는다")
    fun skipsWhenNotEmpty() {
        whenever(articleLinkRepository.count()).thenReturn(5)

        runner.backfillIfEmpty()

        verify(indexer, never()).reindex(any(), any(), any())
    }

    @Test
    @DisplayName("한 문서의 색인이 실패해도 다음 문서를 계속 색인한다")
    fun continuesAfterFailure() {
        whenever(articleLinkRepository.count()).thenReturn(0)
        stubArticles(1L, 2L)
        doThrow(RuntimeException("db")).whenever(indexer).reindex(eq(1L), any(), any())

        runner.backfillIfEmpty()

        verify(indexer).reindex(2L, "문서2", "[[링크]]")
    }
}
