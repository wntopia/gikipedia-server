package io.github.wntopia.gikipedia.server.domain.report.repository

import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleJpaEntity
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.domain.report.entity.ArticleReportJpaEntity
import io.github.wntopia.gikipedia.server.domain.report.entity.ReportStatus
import io.github.wntopia.gikipedia.server.global.config.JpaAuditingConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
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
    fun `신고 목록은 최신 순이고 상태로 거를 수 있으며 null이면 전체다`() {
        val a = article("A 문서")
        val first = reportRepository.save(ArticleReportJpaEntity(10L, a, null, "첫 신고"))
        val second = reportRepository.save(ArticleReportJpaEntity(11L, a, 2, "둘째 신고"))
        first.resolve()
        reportRepository.flush()

        val all = reportRepository.findReports(null, PageRequest.of(0, 10))
        val open = reportRepository.findReports(ReportStatus.OPEN, PageRequest.of(0, 10))
        val resolved = reportRepository.findReports(ReportStatus.RESOLVED, PageRequest.of(0, 10))

        assertThat(all.content.map { it.reason }).containsExactly("둘째 신고", "첫 신고")
        assertThat(all.content.first().articleTitle).isEqualTo("A 문서")
        assertThat(all.content.first().revision).isEqualTo(2)
        assertThat(open.content.map { it.id }).containsExactly(second.id)
        assertThat(resolved.content.map { it.id }).containsExactly(first.id)
        assertThat(all.totalElements).isEqualTo(2)
    }

    @Test
    fun `중복 신고 판단은 처리되지 않은 신고만 본다`() {
        val a = article("A 문서")
        val report = reportRepository.saveAndFlush(ArticleReportJpaEntity(10L, a, null, "사유"))

        assertThat(reportRepository.existsByReporterUserIdAndArticleIdAndStatus(10L, a, ReportStatus.OPEN)).isTrue()

        report.resolve()
        reportRepository.flush()
        assertThat(reportRepository.existsByReporterUserIdAndArticleIdAndStatus(10L, a, ReportStatus.OPEN)).isFalse()
        assertThat(reportRepository.existsByReporterUserIdAndArticleIdAndStatus(99L, a, ReportStatus.OPEN)).isFalse()
    }
}
