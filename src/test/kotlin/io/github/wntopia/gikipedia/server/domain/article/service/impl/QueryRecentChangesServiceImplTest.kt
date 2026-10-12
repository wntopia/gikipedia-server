package io.github.wntopia.gikipedia.server.domain.article.service.impl

import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRecentView
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.domain.user.entity.ContributionJpaEntity
import io.github.wntopia.gikipedia.server.domain.user.entity.ContributionType
import io.github.wntopia.gikipedia.server.domain.user.entity.UserJpaEntity
import io.github.wntopia.gikipedia.server.domain.user.repository.ArticleEditStatsView
import io.github.wntopia.gikipedia.server.domain.user.repository.ContributionRepository
import io.github.wntopia.gikipedia.server.domain.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class QueryRecentChangesServiceImplTest {
    private val articleRepository = mock<ArticleRepository>()
    private val contributionRepository = mock<ContributionRepository>()
    private val userRepository = mock<UserRepository>()

    // KST 2026-10-13 01:00 → 오늘의 시작은 UTC 2026-10-12 15:00
    private val clock = Clock.fixed(Instant.parse("2026-10-12T16:00:00Z"), ZoneOffset.UTC)
    private val service =
        QueryRecentChangesServiceImpl(articleRepository, contributionRepository, userRepository, clock)
    private val kstMidnight = Instant.parse("2026-10-12T15:00:00Z")

    private fun article(
        id: Long,
        title: String,
    ) = object : ArticleRecentView {
        override val id = id
        override val title = title
        override val createdAt: Instant = Instant.EPOCH
        override val updatedAt: Instant = Instant.parse("2026-10-12T16:00:00Z")
    }

    private fun stats(
        articleId: Long,
        edits: Long,
        editors: Long,
    ) = object : ArticleEditStatsView {
        override val articleId = articleId
        override val editCount = edits
        override val editorCount = editors
    }

    @Test
    @DisplayName("문서당 한 항목으로 마지막 기여자·오늘 편집 횟수·편집자 수·신규 생성 여부를 채운다")
    fun groupsPerArticle() {
        whenever(articleRepository.findRecentlyUpdated(any())).thenReturn(
            PageImpl(listOf(article(1L, "수정된 문서"), article(2L, "새 문서")), PageRequest.of(0, 2), 5),
        )
        whenever(contributionRepository.findEditStatsSince(eq(listOf(1L, 2L)), eq(kstMidnight)))
            .thenReturn(listOf(stats(1L, 5, 4)))
        whenever(contributionRepository.findLatestByArticleIds(listOf(1L, 2L))).thenReturn(
            listOf(
                ContributionJpaEntity(10L, 1L, ContributionType.EDIT, 6),
                ContributionJpaEntity(11L, 2L, ContributionType.CREATE),
            ),
        )
        whenever(userRepository.findByDatagsmUserIdIn(setOf(10L, 11L))).thenReturn(
            listOf(UserJpaEntity(10L, "이서연", "USER"), UserJpaEntity(11L, "홍길동", "USER")),
        )

        val result = service.execute(0, 2)

        val edited = result.items[0]
        assertThat(edited.lastEditorName).isEqualTo("이서연")
        assertThat(edited.todayEditCount).isEqualTo(5)
        assertThat(edited.todayEditorCount).isEqualTo(4)
        assertThat(edited.newlyCreated).isFalse()
        val created = result.items[1]
        assertThat(created.newlyCreated).isTrue()
        assertThat(created.todayEditCount).isEqualTo(0)
        assertThat(result.totalElements).isEqualTo(5)
        assertThat(result.hasNext).isTrue()
    }

    @Test
    @DisplayName("비로그인으로 만든 문서는 마지막 기여자 이름 없이 신규 생성으로 나온다")
    fun anonymousCreation() {
        whenever(articleRepository.findRecentlyUpdated(any()))
            .thenReturn(PageImpl(listOf(article(1L, "익명 문서")), PageRequest.of(0, 1), 1))
        whenever(contributionRepository.findEditStatsSince(any(), any())).thenReturn(emptyList())
        whenever(contributionRepository.findLatestByArticleIds(any()))
            .thenReturn(listOf(ContributionJpaEntity(null, 1L, ContributionType.CREATE)))
        whenever(userRepository.findByDatagsmUserIdIn(any())).thenReturn(emptyList())

        val item = service.execute(0, 1).items.single()

        assertThat(item.lastEditorName).isNull()
        assertThat(item.newlyCreated).isTrue()
    }

    @Test
    @DisplayName("문서가 하나도 없으면 원장·사용자 조회 없이 빈 페이지를 반환한다")
    fun emptyPage() {
        whenever(
            articleRepository.findRecentlyUpdated(any()),
        ).thenReturn(PageImpl(emptyList(), PageRequest.of(0, 20), 0))

        val result = service.execute(0, 20)

        assertThat(result.items).isEmpty()
        verify(contributionRepository, never()).findLatestByArticleIds(any())
    }
}
