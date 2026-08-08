package io.github.wntopia.gikipedia.server.domain.history.service

import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleJpaEntity
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.domain.history.dto.ArticleHistorySegmentEntry
import io.github.wntopia.gikipedia.server.domain.history.entity.ArticleHistoryJpaEntity
import io.github.wntopia.gikipedia.server.domain.history.entity.ArticleSnapshotJpaEntity
import io.github.wntopia.gikipedia.server.domain.history.repository.ArticleHistoryRepository
import io.github.wntopia.gikipedia.server.domain.history.repository.ArticleSnapshotRepository
import io.github.wntopia.gikipedia.server.domain.history.service.impl.ReconstructArticleServiceImpl
import io.github.wntopia.gikipedia.server.global.diff.ArticleDiff
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.test.util.ReflectionTestUtils
import team.themoment.sdk.exception.ExpectedException
import java.time.Instant
import java.util.Optional

/**
 * 재구성 불변식 검증: 스냅샷 + 이후 diff 순차 적용이 각 리비전 시점 내용과 정확히 일치하는지 확인한다.
 *
 * 실제 diff 생성/적용은 [ArticleDiff]를 그대로 쓰고, 저장소만 mock으로 대체해 조립 로직만 격리 검증한다.
 */
class ReconstructArticleServiceImplTest {
    private val articleDiff = ArticleDiff()
    private val articleRepository = mock<ArticleRepository>()
    private val historyRepository = mock<ArticleHistoryRepository>()
    private val snapshotRepository = mock<ArticleSnapshotRepository>()
    private val segmentCodec = mock<ArticleHistorySegmentCodec>()

    private lateinit var service: ReconstructArticleServiceImpl

    private val articleId = 1L
    private val article = ArticleJpaEntity(title = "제목", content = "무관")

    /** 리비전별 실제 내용 (테스트 기준값). snapshotInterval=3 가정: 리비전 1, 4가 스냅샷. */
    private val revisionContents =
        listOf(
            "A\nB\nC", // rev1 (스냅샷)
            "A\nB2\nC", // rev2
            "A\nB2\nC\nD", // rev3
            "A\nB2\nC\nD\nE", // rev4 (스냅샷)
            "A\nB2\nX\nD\nE", // rev5
        )

    // 같은 인스턴스를 여러 stub에서 재사용해야, 압축 테스트에서 attachCompressedInteriorPayload로
    // 붙인 페이로드가 이후 조회에서도 그대로 보인다(매번 새로 만들면 mutation이 반영되지 않는다).
    private val snapshot1 = ArticleSnapshotJpaEntity(article, 1, revisionContents[0])
    private val snapshot4 = ArticleSnapshotJpaEntity(article, 4, revisionContents[3])

    @BeforeEach
    fun setUp() {
        ReflectionTestUtils.setField(article, "id", articleId)
        service =
            ReconstructArticleServiceImpl(
                articleRepository,
                historyRepository,
                snapshotRepository,
                segmentCodec,
                articleDiff,
            )

        whenever(articleRepository.findById(articleId)).thenReturn(Optional.of(article))

        // 히스토리: rev1 = baseline diff(""→rev1), rev2~5 = 이전→현재 forward diff
        val histories =
            revisionContents.mapIndexed { idx, content ->
                val revision = idx + 1
                val before = if (idx == 0) "" else revisionContents[idx - 1]
                history(revision, articleDiff.generate(before, content))
            }
        whenever(historyRepository.findByArticleIdOrderByRevisionDesc(articleId))
            .thenReturn(histories.sortedByDescending { it.revision })
        whenever(historyRepository.findTopByArticleIdOrderByRevisionDesc(articleId))
            .thenReturn(histories.maxByOrNull { it.revision })
        whenever(historyRepository.findByArticleIdAndRevisionBetweenOrderByRevisionAsc(any(), any(), any()))
            .thenAnswer { inv ->
                val from = inv.arguments[1] as Int
                val to = inv.arguments[2] as Int
                histories.filter { it.revision in from..to }.sortedBy { it.revision }
            }
        whenever(historyRepository.findByArticleIdAndRevision(any(), any()))
            .thenAnswer { inv ->
                val rev = inv.arguments[1] as Int
                histories.find { it.revision == rev }
            }

        // 스냅샷: rev1, rev4 — 같은 인스턴스를 반환해야 압축 페이로드 mutation이 이후 조회에도 보인다.
        whenever(snapshotRepository.findTopByArticleIdAndRevisionLessThanEqualOrderByRevisionDesc(any(), any()))
            .thenAnswer { inv ->
                val rev = inv.arguments[1] as Int
                listOf(snapshot4, snapshot1).firstOrNull { it.revision <= rev }
            }

        // 기본값: 압축된 스냅샷 없음(listRevisions 테스트에서 필요한 존재 확인 stub 포함).
        whenever(articleRepository.existsById(articleId)).thenReturn(true)
        whenever(snapshotRepository.findByArticleIdAndCompressedInteriorPayloadIsNotNull(articleId))
            .thenReturn(emptyList())
    }

    @Test
    @DisplayName("모든 리비전이 스냅샷+diff로 정확히 재구성된다")
    fun reconstructsEveryRevision() {
        revisionContents.forEachIndexed { idx, expected ->
            val revision = idx + 1
            val result = service.reconstruct(articleId, revision)
            assertThat(result.content)
                .describedAs("revision $revision")
                .isEqualTo(expected)
        }
    }

    @Test
    @DisplayName("스냅샷 경계 리비전(4)은 diff 적용 없이 스냅샷 그대로 복원된다")
    fun snapshotBoundary() {
        val result = service.reconstruct(articleId, 4)
        assertThat(result.content).isEqualTo(revisionContents[3])
    }

    @Test
    @DisplayName("최신 리비전은 latest=true로 표시된다 (캐시 제외 대상)")
    fun latestFlag() {
        assertThat(service.reconstruct(articleId, 5).latest).isTrue()
        assertThat(service.reconstruct(articleId, 3).latest).isFalse()
    }

    @Test
    @DisplayName("존재하지 않는 리비전(범위 밖)은 항상 404를 던진다")
    fun nonExistentRevisionThrowsNotFound() {
        assertThatThrownBy { service.reconstruct(articleId, 0) }.isInstanceOf(ExpectedException::class.java)
        assertThatThrownBy { service.reconstruct(articleId, 6) }.isInstanceOf(ExpectedException::class.java)
    }

    @Test
    @DisplayName("압축된 interior 구간(리비전 2,3)도 스냅샷에 붙은 페이로드에서 복원되어 압축 전과 동일한 content를 반환한다")
    fun reconstructsFromCompactedPayload() {
        // 리비전 2,3의 raw row가 삭제된 것처럼 시뮬레이션(압축 후 상태).
        whenever(historyRepository.findByArticleIdAndRevisionBetweenOrderByRevisionAsc(articleId, 2, 2))
            .thenReturn(emptyList())
        whenever(historyRepository.findByArticleIdAndRevisionBetweenOrderByRevisionAsc(articleId, 2, 3))
            .thenReturn(emptyList())

        val entries =
            listOf(
                ArticleHistorySegmentEntry(2, "2412 홍길동", articleDiff.generate(revisionContents[0], revisionContents[1]), Instant.EPOCH),
                ArticleHistorySegmentEntry(3, "2412 홍길동", articleDiff.generate(revisionContents[1], revisionContents[2]), Instant.EPOCH),
            )
        val payload = byteArrayOf(1, 2, 3)
        snapshot1.attachCompressedInteriorPayload(payload)
        whenever(segmentCodec.decode(payload)).thenReturn(entries)

        assertThat(service.reconstruct(articleId, 2).content).isEqualTo(revisionContents[1])
        assertThat(service.reconstruct(articleId, 3).content).isEqualTo(revisionContents[2])
        // 경계 리비전(1,4)과 latest 판별은 압축 여부와 무관하게 그대로 정확해야 한다.
        assertThat(service.reconstruct(articleId, 4).latest).isFalse()
        assertThat(service.reconstruct(articleId, 5).latest).isTrue()
    }

    @Test
    @DisplayName("listRevisions는 raw row와 압축된 스냅샷 페이로드를 병합해 압축 전과 동일한 목록을 반환한다")
    fun listRevisionsMergesCompactedPayload() {
        val beforeCompaction = service.listRevisions(articleId)

        // 리비전 2,3을 압축 페이로드로 대체(압축 후 상태) — raw 목록에서도 함께 제거된 것처럼 시뮬레이션.
        whenever(historyRepository.findByArticleIdOrderByRevisionDesc(articleId))
            .thenReturn(beforeCompaction.filter { it.revision !in setOf(2, 3) }.map { history(it.revision, "diff") })
        val entries =
            listOf(
                ArticleHistorySegmentEntry(2, "2412 홍길동", "diff-2", Instant.EPOCH),
                ArticleHistorySegmentEntry(3, "2412 홍길동", "diff-3", Instant.EPOCH),
            )
        val payload = byteArrayOf(1, 2, 3)
        snapshot1.attachCompressedInteriorPayload(payload)
        whenever(snapshotRepository.findByArticleIdAndCompressedInteriorPayloadIsNotNull(articleId))
            .thenReturn(listOf(snapshot1))
        whenever(segmentCodec.decode(payload)).thenReturn(entries)

        val afterCompaction = service.listRevisions(articleId)

        assertThat(afterCompaction.map { it.revision }).isEqualTo(beforeCompaction.map { it.revision })
    }

    private fun history(
        revision: Int,
        diff: String,
    ): ArticleHistoryJpaEntity = ArticleHistoryJpaEntity(article, revision, "2412 홍길동", diff)
}
