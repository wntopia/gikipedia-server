package io.github.wntopia.gikipedia.server.domain.auth.service.impl

import io.github.wntopia.gikipedia.server.domain.auth.service.SignoutService
import jakarta.servlet.http.HttpSession
import org.springframework.stereotype.Service

@Service
class SignoutServiceImpl : SignoutService {
    override fun execute(session: HttpSession) {
        session.invalidate()
    }
}
