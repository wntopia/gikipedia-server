package io.github.wntopia.gikipedia.server.domain.history.service

import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleJpaEntity
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.domain.history.entity.ArticleHistoryJpaEntity
import io.github.wntopia.gikipedia.server.domain.history.entity.ArticleSnapshotJpaEntity
import io.github.wntopia.gikipedia.server.domain.history.repository.ArticleHistoryRepository
import io.github.wntopia.gikipedia.server.domain.history.repository.ArticleSnapshotRepository
import io.github.wntopia.gikipedia.server.domain.history.service.impl.ReconstructArticleServiceImpl
import io.github.wntopia.gikipedia.server.global.diff.ArticleDiff
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.test.util.ReflectionTestUtils
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

    @BeforeEach
    fun setUp() {
        ReflectionTestUtils.setField(article, "id", articleId)
        service = ReconstructArticleServiceImpl(articleRepository, historyRepository, snapshotRepository, articleDiff)

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
        whenever(historyRepository.findByArticleIdAndRevisionBetweenOrderByRevisionAsc(any(), any(), any()))
            .thenAnswer { inv ->
                val from = inv.arguments[1] as Int
                val to = inv.arguments[2] as Int
                histories.filter { it.revision in from..to }.sortedBy { it.revision }
            }

        // 스냅샷: rev1, rev4
        whenever(snapshotRepository.findTopByArticleIdAndRevisionLessThanEqualOrderByRevisionDesc(any(), any()))
            .thenAnswer { inv ->
                val rev = inv.arguments[1] as Int
                listOf(4 to revisionContents[3], 1 to revisionContents[0])
                    .firstOrNull { it.first <= rev }
                    ?.let { snapshot(it.first, it.second) }
            }
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

    private fun history(
        revision: Int,
        diff: String,
    ): ArticleHistoryJpaEntity = ArticleHistoryJpaEntity(article, revision, "2412 홍길동", diff)

    private fun snapshot(
        revision: Int,
        content: String,
    ): ArticleSnapshotJpaEntity = ArticleSnapshotJpaEntity(article, revision, content)
}
