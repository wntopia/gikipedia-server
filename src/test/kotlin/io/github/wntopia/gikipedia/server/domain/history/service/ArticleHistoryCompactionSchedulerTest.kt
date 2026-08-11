package io.github.wntopia.gikipedia.server.domain.history.service

import io.github.wntopia.gikipedia.server.domain.history.repository.ArticleSnapshotRepository
import io.github.wntopia.gikipedia.server.domain.history.repository.ArticleSnapshotRevisionView
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

private data class SnapshotRevisionView(
    private val articleId: Long,
    private val revision: Int,
    private val compacted: Boolean,
) : ArticleSnapshotRevisionView {
    override fun getArticleId() = articleId

    override fun getRevision() = revision

    override fun getCompacted() = compacted
}

/** 야간 압축 배치의 대상 탐색(인접 스냅샷 쌍, 이미 압축된 것 제외)과 부분 실패 격리 검증. */
class ArticleHistoryCompactionSchedulerTest {
    private val snapshotRepository = mock<ArticleSnapshotRepository>()
    private val compactor = mock<ArticleHistoryCompactor>()

    private fun scheduler(enabled: Boolean = true) =
        ArticleHistoryCompactionScheduler(snapshotRepository, compactor, enabled)

    @Test
    @DisplayName("아직 압축 안 된 인접 스냅샷 쌍만 압축을 호출하고, open tail은 대상에서 제외한다")
    fun compactsOnlyUncompactedClosedSegments() {
        whenever(snapshotRepository.findAllRevisionsOrderByArticleAscRevisionAsc()).thenReturn(
            listOf(
                SnapshotRevisionView(1L, 1, compacted = true),
                SnapshotRevisionView(1L, 4, compacted = false),
                // 7은 아직 다음 스냅샷이 없는 open tail의 마지막 스냅샷 — from으로는 절대 등장하지 않는다.
                SnapshotRevisionView(1L, 7, compacted = false),
            ),
        )

        scheduler().compact()

        verify(compactor, never()).compactSegment(1L, 1, 4)
        verify(compactor).compactSegment(1L, 4, 7)
    }

    @Test
    @DisplayName("한 세그먼트 압축이 실패해도 나머지 세그먼트는 계속 처리한다")
    fun isolatesPartialFailure() {
        whenever(snapshotRepository.findAllRevisionsOrderByArticleAscRevisionAsc()).thenReturn(
            listOf(
                SnapshotRevisionView(1L, 1, compacted = false),
                SnapshotRevisionView(1L, 4, compacted = false),
                SnapshotRevisionView(2L, 1, compacted = false),
                SnapshotRevisionView(2L, 4, compacted = false),
            ),
        )
        whenever(compactor.compactSegment(1L, 1, 4)).thenThrow(RuntimeException("boom"))

        scheduler().compact()

        verify(compactor).compactSegment(1L, 1, 4)
        verify(compactor).compactSegment(2L, 1, 4)
    }

    @Test
    @DisplayName("탐색 쿼리 자체가 실패해도 배치가 예외를 전파하지 않는다")
    fun survivesDiscoveryQueryFailure() {
        whenever(snapshotRepository.findAllRevisionsOrderByArticleAscRevisionAsc())
            .thenThrow(RuntimeException("db down"))

        scheduler().compact()

        verify(compactor, never()).compactSegment(any(), any(), any())
    }

    @Test
    @DisplayName("압축 기능이 비활성화되어 있으면 탐색조차 하지 않는다")
    fun doesNothingWhenDisabled() {
        scheduler(enabled = false).compact()

        verify(snapshotRepository, never()).findAllRevisionsOrderByArticleAscRevisionAsc()
        verify(compactor, never()).compactSegment(any(), any(), any())
    }
}
