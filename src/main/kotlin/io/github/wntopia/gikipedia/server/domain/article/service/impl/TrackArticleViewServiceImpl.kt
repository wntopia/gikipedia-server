package io.github.wntopia.gikipedia.server.domain.article.service.impl

import io.github.wntopia.gikipedia.server.domain.article.service.ArticleViewCounter
import io.github.wntopia.gikipedia.server.domain.article.service.TrackArticleViewService
import io.github.wntopia.gikipedia.server.global.security.session.AuthenticationReader
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.security.MessageDigest

@Service
class TrackArticleViewServiceImpl(
    private val articleViewCounter: ArticleViewCounter,
    private val authenticationReader: AuthenticationReader,
) : TrackArticleViewService {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun execute(
        articleId: Long,
        request: HttpServletRequest,
    ): Long? =
        try {
            articleViewCounter.recordView(articleId, viewerKey(request))
        } catch (e: Exception) {
            // 조회수는 부가 정보 — 집계(Redis) 장애가 문서 조회를 막아서는 안 된다.
            log.warn("조회수 집계 실패 (articleId={})", articleId, e)
            null
        }

    /**
     * 로그인했으면 계정 id, 아니면 접속 IP와 User-Agent의 해시. 세션을 새로 만들지 않는다 — 비로그인 조회마다 오래 남는
     * 세션이 Redis에 쌓이는 것을 피하기 위함이다. 리버스 프록시 뒤라면 프록시가 실제 클라이언트 주소를 넘기도록
     * (`server.forward-headers-strategy`) 설정해야 비로그인 조회자가 구분된다.
     */
    private fun viewerKey(request: HttpServletRequest): String {
        val session = request.getSession(false)
        val userId = session?.let { authenticationReader.getCurrentUser(it)?.id }
        if (userId != null) return "u:$userId"

        val raw = "${request.remoteAddr}|${request.getHeader("User-Agent").orEmpty()}"
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return "a:" + digest.take(DIGEST_BYTES).joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val DIGEST_BYTES = 12
    }
}
