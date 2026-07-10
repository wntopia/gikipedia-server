package io.github.wntopia.gikipedia.server.domain.auth.service

import jakarta.servlet.http.HttpSession

interface SignoutService {
    fun execute(session: HttpSession)
}
