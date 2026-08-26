package io.github.wntopia.gikipedia.server.domain.auth.service

import io.github.wntopia.gikipedia.server.domain.auth.dto.response.AuthStatusResDto
import jakarta.servlet.http.HttpSession

interface QueryAuthStatusService {
    fun execute(session: HttpSession): AuthStatusResDto
}
