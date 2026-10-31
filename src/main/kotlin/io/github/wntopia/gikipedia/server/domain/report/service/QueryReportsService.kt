package io.github.wntopia.gikipedia.server.domain.report.service

import io.github.wntopia.gikipedia.server.domain.report.dto.response.ReportPageResDto
import io.github.wntopia.gikipedia.server.domain.report.entity.ReportStatus
import jakarta.servlet.http.HttpSession

interface QueryReportsService {
    fun execute(
        status: ReportStatus?,
        page: Int,
        size: Int,
        session: HttpSession,
    ): ReportPageResDto
}
