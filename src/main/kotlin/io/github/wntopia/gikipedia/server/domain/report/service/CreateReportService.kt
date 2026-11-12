package io.github.wntopia.gikipedia.server.domain.report.service

import io.github.wntopia.gikipedia.server.domain.report.dto.request.CreateReportReqDto
import jakarta.servlet.http.HttpSession

interface CreateReportService {
    fun execute(
        articleId: Long,
        revision: Int,
        reqDto: CreateReportReqDto,
        session: HttpSession,
    )
}
