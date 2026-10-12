package io.github.wntopia.gikipedia.server.domain.user.repository

import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleJpaEntity
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.domain.user.entity.ContributionJpaEntity
import io.github.wntopia.gikipedia.server.domain.user.entity.ContributionType
import io.github.wntopia.gikipedia.server.global.config.JpaAuditingConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import java.time.Instant

/** JPQL 조인/프로젝션과 집계 쿼리가 실제 매핑에서 동작하는지 확인한다. */
@DataJpaTest
@Import(JpaAuditingConfig::class)
class ContributionRepositoryIntegrationTest {
    @Autowired
    private lateinit var contributionRepository: ContributionRepository

    @Autowired
    private lateinit var articleRepository: ArticleRepository

    @Test
    fun `최근 기여 목록은 문서 제목과 함께 최신 순으로 내려온다`() {
        val first = requireNotNull(articleRepository.save(ArticleJpaEntity(title = "첫 문서", content = "a")).id)
        val second = requireNotNull(articleRepository.save(ArticleJpaEntity(title = "둘째 문서", content = "b")).id)
        contributionRepository.save(ContributionJpaEntity(10L, first, ContributionType.CREATE))
        contributionRepository.save(ContributionJpaEntity(10L, second, ContributionType.EDIT, 2))
        contributionRepository.save(ContributionJpaEntity(99L, first, ContributionType.EDIT, 2))
        contributionRepository.flush()

        val recent = contributionRepository.findRecentByUserId(10L, PageRequest.of(0, 10))

        assertThat(recent.map { it.title }).containsExactly("둘째 문서", "첫 문서")
        assertThat(recent.map { it.type }).containsExactly(ContributionType.EDIT, ContributionType.CREATE)
    }

    @Test
    fun `구분별 개수와 기준 시각 이후 개수를 센다`() {
        val articleId = requireNotNull(articleRepository.save(ArticleJpaEntity(title = "문서", content = "a")).id)
        contributionRepository.save(ContributionJpaEntity(10L, articleId, ContributionType.CREATE))
        contributionRepository.save(ContributionJpaEntity(10L, articleId, ContributionType.EDIT, 2))
        contributionRepository.save(ContributionJpaEntity(10L, articleId, ContributionType.EDIT, 3))
        contributionRepository.flush()

        assertThat(contributionRepository.countByUserIdAndType(10L, ContributionType.EDIT)).isEqualTo(2)
        assertThat(contributionRepository.countByUserIdAndType(10L, ContributionType.CREATE)).isEqualTo(1)
        assertThat(
            contributionRepository.countByUserIdAndTypeAndCreatedAtGreaterThanEqual(
                10L,
                ContributionType.EDIT,
                Instant.now().minusSeconds(60),
            ),
        ).isEqualTo(2)
        assertThat(
            contributionRepository.countByUserIdAndTypeAndCreatedAtGreaterThanEqual(
                10L,
                ContributionType.EDIT,
                Instant.now().plusSeconds(60),
            ),
        ).isEqualTo(0)
    }

    @Test
    fun `오늘 편집 통계는 수정만 세고 편집한 사용자 수를 중복 없이 센다`() {
        val a = requireNotNull(articleRepository.save(ArticleJpaEntity(title = "A", content = "a")).id)
        val b = requireNotNull(articleRepository.save(ArticleJpaEntity(title = "B", content = "b")).id)
        contributionRepository.save(ContributionJpaEntity(1L, a, ContributionType.CREATE))
        contributionRepository.save(ContributionJpaEntity(1L, a, ContributionType.EDIT, 2))
        contributionRepository.save(ContributionJpaEntity(1L, a, ContributionType.EDIT, 3))
        contributionRepository.save(ContributionJpaEntity(2L, a, ContributionType.EDIT, 4))
        contributionRepository.flush()

        val stats = contributionRepository.findEditStatsSince(listOf(a, b), Instant.now().minusSeconds(60))

        val statsOfA = stats.single { it.articleId == a }
        assertThat(statsOfA.editCount).isEqualTo(3)
        assertThat(statsOfA.editorCount).isEqualTo(2)
        assertThat(stats.none { it.articleId == b }).isTrue()
        assertThat(contributionRepository.findEditStatsSince(listOf(a), Instant.now().plusSeconds(60))).isEmpty()
    }

    @Test
    fun `문서별 가장 최근 기여 한 건만 가져온다`() {
        val a = requireNotNull(articleRepository.save(ArticleJpaEntity(title = "A", content = "a")).id)
        val b = requireNotNull(articleRepository.save(ArticleJpaEntity(title = "B", content = "b")).id)
        contributionRepository.save(ContributionJpaEntity(1L, a, ContributionType.CREATE))
        contributionRepository.save(ContributionJpaEntity(2L, a, ContributionType.EDIT, 2))
        contributionRepository.save(ContributionJpaEntity(null, b, ContributionType.CREATE))
        contributionRepository.flush()

        val latest = contributionRepository.findLatestByArticleIds(listOf(a, b)).associateBy { it.articleId }

        assertThat(latest[a]?.type).isEqualTo(ContributionType.EDIT)
        assertThat(latest[a]?.userId).isEqualTo(2L)
        assertThat(latest[b]?.type).isEqualTo(ContributionType.CREATE)
        assertThat(latest[b]?.userId).isNull()
    }

    @Test
    fun `최근 수정된 문서 순으로 페이지를 가져온다`() {
        val first = articleRepository.saveAndFlush(ArticleJpaEntity(title = "먼저", content = "a"))
        val second = articleRepository.saveAndFlush(ArticleJpaEntity(title = "나중", content = "b"))

        // 다른 테스트 클래스가 커밋해 둔 문서가 같은 DB에 있을 수 있어, 전체가 아니라 이 테스트의 문서끼리의 순서만 본다.
        val page = articleRepository.findRecentlyUpdated(PageRequest.of(0, 1000))
        val ids = page.content.map { it.id }

        assertThat(ids.indexOf(second.id)).isLessThan(ids.indexOf(first.id))
        assertThat(page.totalElements).isGreaterThanOrEqualTo(2)
        assertThat(articleRepository.findRecentlyUpdated(PageRequest.of(0, 1)).hasNext()).isTrue()
    }
}
