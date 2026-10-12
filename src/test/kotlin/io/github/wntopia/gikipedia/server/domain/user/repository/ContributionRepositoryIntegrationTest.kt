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
}
