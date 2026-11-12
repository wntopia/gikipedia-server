package io.github.wntopia.gikipedia.server.domain.report.service

import io.github.wntopia.gikipedia.server.domain.report.dto.response.ReportResDto
import io.github.wntopia.gikipedia.server.global.dto.PageResDto
import jakarta.servlet.http.HttpSession

interface QueryReportsService {
    fun execute(
        session: HttpSession,
        page: Int,
        size: Int,
    ): PageResDto<ReportResDto>
}
