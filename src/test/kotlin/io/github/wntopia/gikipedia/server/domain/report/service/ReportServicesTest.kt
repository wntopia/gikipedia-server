package io.github.wntopia.gikipedia.server.domain.report.service

import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleJpaEntity
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.domain.history.entity.ArticleHistoryJpaEntity
import io.github.wntopia.gikipedia.server.domain.history.repository.ArticleHistoryRepository
import io.github.wntopia.gikipedia.server.domain.report.dto.request.CreateReportReqDto
import io.github.wntopia.gikipedia.server.domain.report.entity.ArticleReportJpaEntity
import io.github.wntopia.gikipedia.server.domain.report.repository.ArticleReportRepository
import io.github.wntopia.gikipedia.server.domain.report.service.impl.CreateReportServiceImpl
import io.github.wntopia.gikipedia.server.domain.report.service.impl.QueryReportsServiceImpl
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
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import team.themoment.sdk.exception.ExpectedException

class ReportServicesTest {
    private val authenticationReader = mock<AuthenticationReader>()
    private val articleRepository = mock<ArticleRepository>()
    private val historyRepository = mock<ArticleHistoryRepository>()
    private val reportRepository = mock<ArticleReportRepository>()
    private val session = mock<HttpSession>()

    private val create =
        CreateReportServiceImpl(authenticationReader, articleRepository, historyRepository, reportRepository)
    private val query = QueryReportsServiceImpl(authenticationReader, reportRepository)

    private val reqDto = CreateReportReqDto("  비방 문구  ")

    private fun givenArticleWithLatestRevision(latest: Int) {
        whenever(authenticationReader.requireUserId(session)).thenReturn(10L)
        whenever(articleRepository.existsById(1L)).thenReturn(true)
        whenever(historyRepository.findTopByArticleIdOrderByRevisionDesc(1L))
            .thenReturn(ArticleHistoryJpaEntity(ArticleJpaEntity("t", "c"), latest, "2412 홍길동", ""))
    }

    private fun statusOf(block: () -> Unit) = (runCatching(block).exceptionOrNull() as ExpectedException).statusCode

    @Test
    @DisplayName("로그인한 사용자가 리비전을 신고하면 사유를 다듬어 저장한다")
    fun createsReport() {
        givenArticleWithLatestRevision(3)

        create.execute(1L, 2, reqDto, session)

        val saved =
            argumentCaptor<ArticleReportJpaEntity>()
                .apply {
                    verify(reportRepository).saveAndFlush(capture())
                }.firstValue
        assertThat(listOf(saved.userId, saved.articleId, saved.revision.toLong())).containsExactly(10L, 1L, 2L)
        assertThat(saved.reason).isEqualTo("비방 문구")
    }

    @Test
    @DisplayName("로그인하지 않으면 401")
    fun rejectsAnonymous() {
        whenever(
            authenticationReader.requireUserId(session),
        ).thenThrow(ExpectedException("인증이 필요합니다.", HttpStatus.UNAUTHORIZED))

        assertThat(statusOf { create.execute(1L, 1, reqDto, session) }).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    @DisplayName("문서가 없거나 리비전이 범위를 벗어나면 404")
    fun rejectsMissingTarget() {
        givenArticleWithLatestRevision(3)
        whenever(articleRepository.existsById(2L)).thenReturn(false)

        assertThat(statusOf { create.execute(2L, 1, reqDto, session) }).isEqualTo(HttpStatus.NOT_FOUND)
        assertThat(statusOf { create.execute(1L, 0, reqDto, session) }).isEqualTo(HttpStatus.NOT_FOUND)
        assertThat(statusOf { create.execute(1L, 4, reqDto, session) }).isEqualTo(HttpStatus.NOT_FOUND)
        verify(reportRepository, never()).saveAndFlush(any<ArticleReportJpaEntity>())
    }

    @Test
    @DisplayName("같은 리비전을 다시 신고하면 409 — 동시 요청으로 유니크 제약에 걸려도 409")
    fun rejectsDuplicate() {
        givenArticleWithLatestRevision(3)
        whenever(reportRepository.existsByUserIdAndArticleIdAndRevision(10L, 1L, 2)).thenReturn(true)
        assertThat(statusOf { create.execute(1L, 2, reqDto, session) }).isEqualTo(HttpStatus.CONFLICT)

        whenever(reportRepository.existsByUserIdAndArticleIdAndRevision(10L, 1L, 2)).thenReturn(false)
        doThrow(
            DataIntegrityViolationException("dup"),
        ).whenever(reportRepository).saveAndFlush(any<ArticleReportJpaEntity>())
        assertThat(statusOf { create.execute(1L, 2, reqDto, session) }).isEqualTo(HttpStatus.CONFLICT)
    }

    @Test
    @DisplayName("관리자가 아니면 신고 목록을 조회할 수 없다")
    fun listRequiresAdmin() {
        whenever(
            authenticationReader.requireAdmin(session),
        ).thenThrow(ExpectedException("관리자만 접근할 수 있습니다.", HttpStatus.FORBIDDEN))

        assertThatThrownBy { query.execute(session, 0, 20) }.isInstanceOf(ExpectedException::class.java)
        verify(reportRepository, never()).findAllWithTitle(any())
    }
}
