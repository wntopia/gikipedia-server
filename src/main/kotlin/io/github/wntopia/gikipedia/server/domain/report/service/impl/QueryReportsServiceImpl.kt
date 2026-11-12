package io.github.wntopia.gikipedia.server.domain.report.service.impl

import io.github.wntopia.gikipedia.server.domain.report.dto.response.ReportResDto
import io.github.wntopia.gikipedia.server.domain.report.repository.ArticleReportRepository
import io.github.wntopia.gikipedia.server.domain.report.service.QueryReportsService
import io.github.wntopia.gikipedia.server.global.dto.PageResDto
import io.github.wntopia.gikipedia.server.global.security.session.AuthenticationReader
import jakarta.servlet.http.HttpSession
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class QueryReportsServiceImpl(
    private val authenticationReader: AuthenticationReader,
    private val articleReportRepository: ArticleReportRepository,
) : QueryReportsService {
    @Transactional(readOnly = true)
    override fun execute(
        session: HttpSession,
        page: Int,
        size: Int,
    ): PageResDto<ReportResDto> {
        authenticationReader.requireAdmin(session)
        val result = articleReportRepository.findAllWithTitle(PageRequest.of(page, size))
        return PageResDto(result.content.map(ReportResDto::from), page, size, result.totalElements, result.hasNext())
    }
}
