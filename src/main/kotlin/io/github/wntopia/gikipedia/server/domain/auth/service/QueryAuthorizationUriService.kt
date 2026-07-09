package io.github.wntopia.gikipedia.server.domain.auth.service

import jakarta.servlet.http.HttpSession
import org.springframework.http.ResponseEntity

interface QueryAuthorizationUriService {
    fun execute(session: HttpSession): ResponseEntity<Void>
}
