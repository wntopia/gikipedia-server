package io.github.wntopia.gikipedia.server.domain.auth.service.impl

import io.github.wntopia.gikipedia.server.domain.auth.service.QueryAuthorizationUriService
import io.github.wntopia.gikipedia.server.global.config.DataGsmOAuthEnvironment
import io.github.wntopia.gikipedia.server.global.security.session.OAuthSessionAttributes
import jakarta.servlet.http.HttpSession
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import team.themoment.datagsm.sdk.oauth.DataGsmOAuthClient
import java.net.URI
import java.util.UUID

@Service
class QueryAuthorizationUriServiceImpl(
    private val environment: DataGsmOAuthEnvironment,
    private val client: DataGsmOAuthClient,
) : QueryAuthorizationUriService {
    override fun execute(session: HttpSession): ResponseEntity<Void> = redirect(createAuthorizationRedirect(session))

    private fun createAuthorizationRedirect(session: HttpSession): URI {
        val state = UUID.randomUUID().toString()
        val builder = client.createAuthorizationUrl(environment.redirectUri()).state(state).enablePkce()

        if (StringUtils.hasText(environment.scope())) {
            builder.scope(environment.scope())
        }

        session.setAttribute(OAuthSessionAttributes.DATAGSM_STATE, state)
        session.setAttribute(OAuthSessionAttributes.DATAGSM_CODE_VERIFIER, builder.codeVerifier)

        return URI.create(builder.build())
    }

    private fun redirect(uri: URI): ResponseEntity<Void> =
        ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, uri.toString()).build()
}
