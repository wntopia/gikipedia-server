package io.github.wntopia.gikipedia.server.domain.report.service

import jakarta.servlet.http.HttpSession

interface ResolveReportService {
    fun execute(
        reportId: Long,
        session: HttpSession,
    )
}
