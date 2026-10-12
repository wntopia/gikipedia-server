package io.github.wntopia.gikipedia.server.domain.report.service

import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.domain.auth.dto.response.DataGsmUserInfoResDto
import io.github.wntopia.gikipedia.server.domain.history.entity.ArticleHistoryJpaEntity
import io.github.wntopia.gikipedia.server.domain.history.repository.ArticleHistoryRepository
import io.github.wntopia.gikipedia.server.domain.report.dto.request.CreateReportReqDto
import io.github.wntopia.gikipedia.server.domain.report.entity.ArticleReportJpaEntity
import io.github.wntopia.gikipedia.server.domain.report.entity.ReportStatus
import io.github.wntopia.gikipedia.server.domain.report.repository.ArticleReportRepository
import io.github.wntopia.gikipedia.server.domain.report.service.impl.CreateReportServiceImpl
import io.github.wntopia.gikipedia.server.domain.report.service.impl.QueryReportsServiceImpl
import io.github.wntopia.gikipedia.server.domain.report.service.impl.ResolveReportServiceImpl
import io.github.wntopia.gikipedia.server.global.security.session.AuthenticationReader
import jakarta.servlet.http.HttpSession
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import team.themoment.sdk.exception.ExpectedException
import java.util.Optional

class ReportServicesTest {
    private val authenticationReader = mock<AuthenticationReader>()
    private val articleRepository = mock<ArticleRepository>()
    private val historyRepository = mock<ArticleHistoryRepository>()
    private val reportRepository = mock<ArticleReportRepository>()
    private val session = mock<HttpSession>()

    private val create =
        CreateReportServiceImpl(authenticationReader, articleRepository, historyRepository, reportRepository)
    private val query = QueryReportsServiceImpl(authenticationReader, reportRepository)
    private val resolve = ResolveReportServiceImpl(authenticationReader, reportRepository)

    private fun status(e: Throwable) = (e as ExpectedException).statusCode

    private fun loggedIn(userId: Long = 10L) {
        whenever(
            authenticationReader.getCurrentUser(session),
        ).thenReturn(DataGsmUserInfoResDto(userId, null, "USER", true, null))
    }

    @Test
    @DisplayName("로그인한 사용자가 문서를 신고하면 사유를 다듬어 접수한다")
    fun createsReport() {
        loggedIn()
        whenever(articleRepository.existsById(1L)).thenReturn(true)

        create.execute(1L, CreateReportReqDto("  허위 정보  "), session)

        val captor = argumentCaptor<ArticleReportJpaEntity>()
        verify(reportRepository).save(captor.capture())
        assertThat(captor.firstValue.reporterUserId).isEqualTo(10L)
        assertThat(captor.firstValue.reason).isEqualTo("허위 정보")
        assertThat(captor.firstValue.revision).isNull()
        assertThat(captor.firstValue.status).isEqualTo(ReportStatus.OPEN)
    }

    @Test
    @DisplayName("비로그인은 401, 없는 문서는 404")
    fun rejectsAnonymousAndMissingArticle() {
        whenever(authenticationReader.getCurrentUser(session)).thenReturn(null)
        assertThatThrownBy { create.execute(1L, CreateReportReqDto("사유"), session) }
            .satisfies({ assertThat(status(it)).isEqualTo(HttpStatus.UNAUTHORIZED) })

        loggedIn()
        whenever(articleRepository.existsById(9L)).thenReturn(false)
        assertThatThrownBy { create.execute(9L, CreateReportReqDto("사유"), session) }
            .satisfies({ assertThat(status(it)).isEqualTo(HttpStatus.NOT_FOUND) })
        verify(reportRepository, never()).save(any<ArticleReportJpaEntity>())
    }

    @Test
    @DisplayName("같은 문서에 처리되지 않은 신고가 이미 있으면 409")
    fun rejectsDuplicateOpenReport() {
        loggedIn()
        whenever(articleRepository.existsById(1L)).thenReturn(true)
        whenever(
            reportRepository.existsByReporterUserIdAndArticleIdAndStatus(10L, 1L, ReportStatus.OPEN),
        ).thenReturn(true)

        assertThatThrownBy { create.execute(1L, CreateReportReqDto("사유"), session) }
            .satisfies({ assertThat(status(it)).isEqualTo(HttpStatus.CONFLICT) })
        verify(reportRepository, never()).save(any<ArticleReportJpaEntity>())
    }

    @Test
    @DisplayName("존재하는 리비전은 대상으로 접수하고, 최신보다 큰 리비전은 400")
    fun validatesRevision() {
        loggedIn()
        whenever(articleRepository.existsById(1L)).thenReturn(true)
        val latest = mock<ArticleHistoryJpaEntity>()
        whenever(latest.revision).thenReturn(4)
        whenever(historyRepository.findTopByArticleIdOrderByRevisionDesc(1L)).thenReturn(latest)

        create.execute(1L, CreateReportReqDto("사유", 4), session)
        val captor = argumentCaptor<ArticleReportJpaEntity>()
        verify(reportRepository).save(captor.capture())
        assertThat(captor.firstValue.revision).isEqualTo(4)

        assertThatThrownBy { create.execute(1L, CreateReportReqDto("사유", 5), session) }
            .satisfies({ assertThat(status(it)).isEqualTo(HttpStatus.BAD_REQUEST) })
    }

    @Test
    @DisplayName("히스토리가 없는 문서는 리비전 1만 신고할 수 있다")
    fun untouchedArticleHasOnlyRevisionOne() {
        loggedIn()
        whenever(articleRepository.existsById(1L)).thenReturn(true)
        whenever(historyRepository.findTopByArticleIdOrderByRevisionDesc(1L)).thenReturn(null)

        create.execute(1L, CreateReportReqDto("사유", 1), session)
        assertThatThrownBy { create.execute(1L, CreateReportReqDto("사유", 2), session) }
            .satisfies({ assertThat(status(it)).isEqualTo(HttpStatus.BAD_REQUEST) })
    }

    @Test
    @DisplayName("관리자가 아니면 신고 목록·처리가 403이고 아무것도 조회·변경하지 않는다")
    fun nonAdminForbidden() {
        doThrow(
            ExpectedException("관리자만 접근할 수 있습니다.", HttpStatus.FORBIDDEN),
        ).whenever(authenticationReader).requireAdmin(session)

        assertThatThrownBy { query.execute(null, 0, 20, session) }
            .satisfies({ assertThat(status(it)).isEqualTo(HttpStatus.FORBIDDEN) })
        assertThatThrownBy { resolve.execute(1L, session) }
            .satisfies({ assertThat(status(it)).isEqualTo(HttpStatus.FORBIDDEN) })
        verify(reportRepository, never()).findReports(any(), any())
        verify(reportRepository, never()).findById(any<Long>())
    }

    @Test
    @DisplayName("관리자는 상태 필터로 신고 목록을 조회한다")
    fun adminListsReports() {
        whenever(reportRepository.findReports(ReportStatus.OPEN, PageRequest.of(0, 20)))
            .thenReturn(PageImpl(emptyList(), PageRequest.of(0, 20), 0))

        val result = query.execute(ReportStatus.OPEN, 0, 20, session)

        verify(authenticationReader).requireAdmin(session)
        assertThat(result.items).isEmpty()
        assertThat(result.hasNext).isFalse()
    }

    @Test
    @DisplayName("관리자가 신고를 처리 완료로 바꾸고, 이미 처리된 신고도 오류 없이 끝난다")
    fun resolvesReport() {
        val report = ArticleReportJpaEntity(10L, 1L, null, "사유")
        whenever(reportRepository.findById(5L)).thenReturn(Optional.of(report))

        resolve.execute(5L, session)
        resolve.execute(5L, session)

        assertThat(report.status).isEqualTo(ReportStatus.RESOLVED)
    }

    @Test
    @DisplayName("없는 신고를 처리하면 404")
    fun resolveUnknownReport() {
        whenever(reportRepository.findById(404L)).thenReturn(Optional.empty())

        assertThatThrownBy { resolve.execute(404L, session) }
            .satisfies({ assertThat(status(it)).isEqualTo(HttpStatus.NOT_FOUND) })
    }
}
