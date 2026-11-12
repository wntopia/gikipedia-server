package io.github.wntopia.gikipedia.server.domain.report.repository

import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleJpaEntity
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.domain.report.entity.ArticleReportJpaEntity
import io.github.wntopia.gikipedia.server.global.config.JpaAuditingConfig
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest

@DataJpaTest
@Import(JpaAuditingConfig::class)
class ArticleReportRepositoryIntegrationTest {
    @Autowired
    private lateinit var reportRepository: ArticleReportRepository

    @Autowired
    private lateinit var articleRepository: ArticleRepository

    private fun article(title: String) =
        requireNotNull(articleRepository.saveAndFlush(ArticleJpaEntity(title = title, content = "x")).id)

    @Test
    fun `같은 사용자가 같은 리비전을 두 번 신고하면 유니크 제약이 막는다`() {
        val a = article("A")
        reportRepository.saveAndFlush(ArticleReportJpaEntity(10L, a, 1, "사유"))

        assertThatThrownBy { reportRepository.saveAndFlush(ArticleReportJpaEntity(10L, a, 1, "다른 사유")) }
            .isInstanceOf(DataIntegrityViolationException::class.java)
    }

    @Test
    fun `신고 목록은 최근 접수 순으로 문서 제목과 함께 내려준다`() {
        val a = article("A 문서")
        val b = article("B 문서")
        reportRepository.save(ArticleReportJpaEntity(10L, a, 1, "첫 신고"))
        reportRepository.save(ArticleReportJpaEntity(10L, a, 2, "둘째 신고"))
        reportRepository.save(ArticleReportJpaEntity(99L, b, 1, "셋째 신고"))
        reportRepository.flush()

        val page = reportRepository.findAllWithTitle(PageRequest.of(0, 2))

        assertThat(page.content.map { it.title to it.reason }).containsExactly("B 문서" to "셋째 신고", "A 문서" to "둘째 신고")
        assertThat(page.totalElements).isEqualTo(3)
        assertThat(reportRepository.existsByUserIdAndArticleIdAndRevision(10L, a, 2)).isTrue()
        assertThat(reportRepository.existsByUserIdAndArticleIdAndRevision(99L, a, 2)).isFalse()
    }
}
