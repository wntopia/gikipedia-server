package io.github.wntopia.gikipedia.server.domain.history.service

import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleJpaEntity
import io.github.wntopia.gikipedia.server.domain.history.dto.ArticleHistorySegmentEntry
import io.github.wntopia.gikipedia.server.domain.history.entity.ArticleHistoryJpaEntity
import io.github.wntopia.gikipedia.server.domain.history.entity.ArticleSnapshotJpaEntity
import io.github.wntopia.gikipedia.server.domain.history.repository.ArticleHistoryRepository
import io.github.wntopia.gikipedia.server.domain.history.repository.ArticleSnapshotRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.test.util.ReflectionTestUtils
import java.time.Instant

/** 스냅샷 구간(fromRevision, toRevision)의 interior 리비전을 압축해 fromRevision 스냅샷에 붙이는 로직 검증. */
class ArticleHistoryCompactorTest {
    private val articleHistoryRepository = mock<ArticleHistoryRepository>()
    private val articleSnapshotRepository = mock<ArticleSnapshotRepository>()
    private val codec = mock<ArticleHistorySegmentCodec>()

    private val compactor = ArticleHistoryCompactor(articleHistoryRepository, articleSnapshotRepository, codec)

    private val articleId = 1L
    private val article = ArticleJpaEntity(title = "제목", content = "무관")

    @BeforeEach
    fun setUp() {
        ReflectionTestUtils.setField(article, "id", articleId)
    }

    private fun history(
        revision: Int,
        diff: String = "diff-$revision",
    ) = ArticleHistoryJpaEntity(article, revision, "2412 홍길동", diff).also {
        // compactSegment()가 createdAt을 requireNotNull로 확인하므로, 실제 영속화 후 JPA 감사가
        // 채우는 값을 테스트에서도 미리 채워둔다.
        ReflectionTestUtils.setField(it, "createdAt", Instant.now())
    }

    private fun snapshot(revision: Int) = ArticleSnapshotJpaEntity(article, revision, "content-$revision")

    @Test
    @DisplayName("interior 리비전들을 압축해 fromRevision 스냅샷에 붙이고 원본 row를 삭제한다")
    fun compactsInteriorRevisions() {
        val fromSnapshot = snapshot(1)
        whenever(articleSnapshotRepository.findByArticleIdAndRevision(articleId, 1)).thenReturn(fromSnapshot)
        val interior = listOf(history(2), history(3))
        whenever(articleHistoryRepository.findByArticleIdAndRevisionBetweenOrderByRevisionAsc(articleId, 2, 3))
            .thenReturn(interior)
        val encoded = byteArrayOf(1, 2, 3)
        whenever(codec.encode(any())).thenReturn(encoded)
        whenever(articleHistoryRepository.deleteInteriorRevisions(articleId, 2, 3)).thenReturn(2)

        compactor.compactSegment(articleId, fromRevision = 1, toRevision = 4)

        assertThat(fromSnapshot.compressedInteriorPayload).isEqualTo(encoded)
        verify(articleSnapshotRepository).save(fromSnapshot)

        val entriesCaptor = argumentCaptor<List<ArticleHistorySegmentEntry>>()
        verify(codec).encode(entriesCaptor.capture())
        assertThat(entriesCaptor.firstValue.map { it.revision }).containsExactly(2, 3)

        verify(articleHistoryRepository).deleteInteriorRevisions(articleId, 2, 3)
    }

    @Test
    @DisplayName("interior가 없는 구간(연속 스냅샷)은 아무 것도 저장/삭제하지 않는다")
    fun skipsWhenNoInterior() {
        compactor.compactSegment(articleId, fromRevision = 1, toRevision = 2)

        verify(articleSnapshotRepository, never()).save(any())
        verify(articleHistoryRepository, never()).deleteInteriorRevisions(any(), any(), any())
    }

    @Test
    @DisplayName("압축 대상 스냅샷 row를 찾을 수 없으면 스킵한다")
    fun skipsWhenSnapshotMissing() {
        whenever(articleSnapshotRepository.findByArticleIdAndRevision(articleId, 1)).thenReturn(null)

        compactor.compactSegment(articleId, fromRevision = 1, toRevision = 4)

        verify(articleSnapshotRepository, never()).save(any())
        verify(articleHistoryRepository, never()).deleteInteriorRevisions(any(), any(), any())
    }

    @Test
    @DisplayName("이미 압축된 구간이면 스킵한다")
    fun skipsWhenAlreadyCompacted() {
        val fromSnapshot = snapshot(1)
        fromSnapshot.attachCompressedInteriorPayload(byteArrayOf(9))
        whenever(articleSnapshotRepository.findByArticleIdAndRevision(articleId, 1)).thenReturn(fromSnapshot)

        compactor.compactSegment(articleId, fromRevision = 1, toRevision = 4)

        verify(articleSnapshotRepository, never()).save(any())
        verify(articleHistoryRepository, never()).deleteInteriorRevisions(any(), any(), any())
    }

    @Test
    @DisplayName("interior row 개수가 기대치와 다르면 방어적으로 압축을 스킵한다")
    fun skipsOnCountMismatch() {
        whenever(articleSnapshotRepository.findByArticleIdAndRevision(articleId, 1)).thenReturn(snapshot(1))
        whenever(articleHistoryRepository.findByArticleIdAndRevisionBetweenOrderByRevisionAsc(articleId, 2, 3))
            .thenReturn(listOf(history(2))) // 기대치는 2건(2,3)인데 1건만 있음

        compactor.compactSegment(articleId, fromRevision = 1, toRevision = 4)

        verify(articleSnapshotRepository, never()).save(any())
        verify(articleHistoryRepository, never()).deleteInteriorRevisions(any(), any(), any())
    }
}
