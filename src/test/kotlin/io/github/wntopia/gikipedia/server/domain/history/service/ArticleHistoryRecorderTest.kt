package io.github.wntopia.gikipedia.server.domain.history.service

import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleJpaEntity
import io.github.wntopia.gikipedia.server.domain.history.entity.ArticleHistoryJpaEntity
import io.github.wntopia.gikipedia.server.domain.history.entity.ArticleSnapshotJpaEntity
import io.github.wntopia.gikipedia.server.domain.history.event.ArticleUpdatedEvent
import io.github.wntopia.gikipedia.server.domain.history.repository.ArticleHistoryRepository
import io.github.wntopia.gikipedia.server.domain.history.repository.ArticleSnapshotRepository
import io.github.wntopia.gikipedia.server.global.diff.ArticleDiff
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
import org.springframework.context.ApplicationEventPublisher
import org.springframework.test.util.ReflectionTestUtils
import java.time.Instant

/** 히스토리 기록 로직 검증: 최초 수정 baseline 확정, 스냅샷 주기, 변화 없음 처리, 이벤트 발행. */
class ArticleHistoryRecorderTest {
    private val articleDiff = ArticleDiff()
    private val historyRepository = mock<ArticleHistoryRepository>()
    private val snapshotRepository = mock<ArticleSnapshotRepository>()
    private val eventPublisher = mock<ApplicationEventPublisher>()

    // snapshotInterval = 3 → 스냅샷 리비전: 1, 4, 7...
    private val recorder =
        ArticleHistoryRecorder(historyRepository, snapshotRepository, articleDiff, eventPublisher, 3)

    private val article = ArticleJpaEntity(title = "제목", content = "무관")

    @BeforeEach
    fun setUp() {
        ReflectionTestUtils.setField(article, "id", 1L)
        // 이벤트 발행 시 buildEvent()가 createdAt/updatedAt을 requireNotNull로 확인하므로,
        // 실제 영속화 후 JPA 감사가 채우는 값을 테스트에서도 미리 채워둔다.
        ReflectionTestUtils.setField(article, "createdAt", Instant.now())
        ReflectionTestUtils.setField(article, "updatedAt", Instant.now())
    }

    @Test
    @DisplayName("최초 수정 시 생성 시점 내용을 리비전 1 스냅샷으로 확정하고 새 내용을 리비전 2로 쌓는다")
    fun firstEditCreatesBaseline() {
        whenever(historyRepository.findTopByArticleIdOrderByRevisionDesc(1L)).thenReturn(null)

        val newRevision = recorder.record(article, before = "원본", after = "수정본", editor = "2412 홍길동")

        assertThat(newRevision).isEqualTo(2)

        val historyCaptor = argumentCaptor<ArticleHistoryJpaEntity>()
        verify(historyRepository, org.mockito.kotlin.times(2)).save(historyCaptor.capture())
        assertThat(historyCaptor.allValues.map { it.revision }).containsExactly(1, 2)

        val snapshotCaptor = argumentCaptor<ArticleSnapshotJpaEntity>()
        verify(snapshotRepository).save(snapshotCaptor.capture())
        assertThat(snapshotCaptor.firstValue.revision).isEqualTo(1)
        assertThat(snapshotCaptor.firstValue.content).isEqualTo("원본")
    }

    @Test
    @DisplayName("content 변화가 없으면 리비전을 만들지 않지만, 이미지 등 다른 필드가 바뀌었을 수 있으므로 현재 리비전으로 동기화 이벤트는 다시 발행한다")
    fun noContentChangeStillPublishesEventWithCurrentRevision() {
        whenever(historyRepository.findTopByArticleIdOrderByRevisionDesc(1L))
            .thenReturn(ArticleHistoryJpaEntity(article, 4, "e", "diff"))

        val newRevision = recorder.record(article, before = "같음", after = "같음", editor = "e")

        assertThat(newRevision).isNull()
        verify(historyRepository, never()).save(any())

        val eventCaptor = argumentCaptor<ArticleUpdatedEvent>()
        verify(eventPublisher).publishEvent(eventCaptor.capture())
        assertThat(eventCaptor.firstValue.articleId).isEqualTo(1L)
        assertThat(eventCaptor.firstValue.revision).isEqualTo(4)
    }

    @Test
    @DisplayName("한 번도 수정 안 된 문서에서 content 변화가 없으면 baseline 리비전(1)으로 동기화 이벤트를 발행한다")
    fun noContentChangeOnNeverEditedArticlePublishesBaselineRevision() {
        whenever(historyRepository.findTopByArticleIdOrderByRevisionDesc(1L)).thenReturn(null)

        val newRevision = recorder.record(article, before = "같음", after = "같음", editor = "e")

        assertThat(newRevision).isNull()
        val eventCaptor = argumentCaptor<ArticleUpdatedEvent>()
        verify(eventPublisher).publishEvent(eventCaptor.capture())
        assertThat(eventCaptor.firstValue.revision).isEqualTo(1)
    }

    @Test
    @DisplayName("스냅샷 주기(interval=3)에 해당하는 리비전에서만 스냅샷을 저장한다")
    fun snapshotOnlyAtInterval() {
        // 직전 최신 리비전이 3 → 이번엔 리비전 4 (스냅샷 대상)
        whenever(historyRepository.findTopByArticleIdOrderByRevisionDesc(1L))
            .thenReturn(ArticleHistoryJpaEntity(article, 3, "e", "diff"))

        val newRevision = recorder.record(article, before = "X", after = "Y", editor = "e")

        assertThat(newRevision).isEqualTo(4)
        val snapshotCaptor = argumentCaptor<ArticleSnapshotJpaEntity>()
        verify(snapshotRepository).save(snapshotCaptor.capture())
        assertThat(snapshotCaptor.firstValue.revision).isEqualTo(4)
    }

    @Test
    @DisplayName("스냅샷 주기가 아닌 리비전에서는 diff만 쌓고 스냅샷은 저장하지 않는다")
    fun noSnapshotOffInterval() {
        // 직전 최신 리비전이 4 → 이번엔 리비전 5 (스냅샷 아님)
        whenever(historyRepository.findTopByArticleIdOrderByRevisionDesc(1L))
            .thenReturn(ArticleHistoryJpaEntity(article, 4, "e", "diff"))

        val newRevision = recorder.record(article, before = "X", after = "Y", editor = "e")

        assertThat(newRevision).isEqualTo(5)
        verify(snapshotRepository, never()).save(any())
    }

    @Test
    @DisplayName("새 리비전 커밋 후 Mongo 동기화 이벤트를 발행한다")
    fun publishesEvent() {
        whenever(historyRepository.findTopByArticleIdOrderByRevisionDesc(1L))
            .thenReturn(ArticleHistoryJpaEntity(article, 4, "e", "diff"))

        recorder.record(article, before = "X", after = "Y", editor = "e")

        val eventCaptor = argumentCaptor<ArticleUpdatedEvent>()
        verify(eventPublisher).publishEvent(eventCaptor.capture())
        assertThat(eventCaptor.firstValue.articleId).isEqualTo(1L)
        assertThat(eventCaptor.firstValue.revision).isEqualTo(5)
    }
}
