package io.github.wntopia.gikipedia.server.domain.article.service

import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import java.util.function.Consumer

class ArticleViewFlushSchedulerTest {
    private val counter = mock<ArticleViewCounter>()
    private val articleRepository = mock<ArticleRepository>()
    private val transactionTemplate = mock<TransactionTemplate>()
    private val scheduler = ArticleViewFlushScheduler(counter, articleRepository, transactionTemplate)

    private fun runCallbacksInline() {
        whenever(transactionTemplate.executeWithoutResult(any<Consumer<TransactionStatus>>())).thenAnswer {
            it.getArgument<Consumer<TransactionStatus>>(0).accept(mock())
            null
        }
    }

    @Test
    @DisplayName("증가분을 문서별로 MySQL에 반영한 뒤 Redis의 증가분을 지운다")
    fun flushesAndConfirms() {
        whenever(counter.drainPending()).thenReturn(mapOf(1L to 3L, 2L to 4L))
        runCallbacksInline()

        scheduler.flush()

        verify(articleRepository).addViewCount(1L, 3L)
        verify(articleRepository).addViewCount(2L, 4L)
        verify(counter).confirmDrained()
    }

    @Test
    @DisplayName("반영할 증가분이 없으면 아무것도 하지 않는다")
    fun noopWhenNothingPending() {
        whenever(counter.drainPending()).thenReturn(emptyMap())

        scheduler.flush()

        verify(articleRepository, never()).addViewCount(any(), any())
        verify(counter, never()).confirmDrained()
    }

    @Test
    @DisplayName("반영이 실패하면 증가분을 지우지 않아 다음 주기에 이어서 반영한다")
    fun keepsPendingOnFailure() {
        whenever(counter.drainPending()).thenReturn(mapOf(1L to 3L))
        whenever(transactionTemplate.executeWithoutResult(any<Consumer<TransactionStatus>>()))
            .thenThrow(RuntimeException("db down"))

        scheduler.flush()

        verify(counter, never()).confirmDrained()
    }
}
