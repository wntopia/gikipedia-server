package io.github.wntopia.gikipedia.server.domain.report.service.impl

import io.github.wntopia.gikipedia.server.domain.report.repository.ArticleReportRepository
import io.github.wntopia.gikipedia.server.domain.report.service.ResolveReportService
import io.github.wntopia.gikipedia.server.global.security.session.AuthenticationReader
import jakarta.servlet.http.HttpSession
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import team.themoment.sdk.exception.ExpectedException

@Service
class ResolveReportServiceImpl(
    private val authenticationReader: AuthenticationReader,
    private val articleReportRepository: ArticleReportRepository,
) : ResolveReportService {
    /** 이미 처리된 신고를 다시 처리해도 성공으로 끝난다(멱등). */
    @Transactional
    override fun execute(
        reportId: Long,
        session: HttpSession,
    ) {
        authenticationReader.requireAdmin(session)
        val report =
            articleReportRepository
                .findById(reportId)
                .orElseThrow { ExpectedException("존재하지 않는 신고입니다.", HttpStatus.NOT_FOUND) }
        report.resolve()
    }
}
