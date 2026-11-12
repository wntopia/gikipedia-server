package io.github.wntopia.gikipedia.server.domain.article.service.impl

import io.github.wntopia.gikipedia.server.domain.article.service.ArticleViewCounter
import io.github.wntopia.gikipedia.server.domain.auth.dto.response.DataGsmUserInfoResDto
import io.github.wntopia.gikipedia.server.global.security.session.AuthenticationReader
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpSession
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class TrackArticleViewServiceImplTest {
    private val counter = mock<ArticleViewCounter>()
    private val authenticationReader = mock<AuthenticationReader>()
    private val service = TrackArticleViewServiceImpl(counter, authenticationReader)

    private fun request(
        session: HttpSession?,
        ip: String = "10.0.0.1",
    ) = mock<HttpServletRequest>().also {
        whenever(it.getSession(false)).thenReturn(session)
        whenever(it.remoteAddr).thenReturn(ip)
        whenever(it.getHeader("User-Agent")).thenReturn("JUnit")
    }

    @Test
    @DisplayName("로그인한 사용자는 계정 id로 구분한다")
    fun keysByUserId() {
        val session = mock<HttpSession>()
        whenever(
            authenticationReader.getCurrentUser(session),
        ).thenReturn(DataGsmUserInfoResDto(10L, null, "USER", true, null))
        whenever(counter.recordView(eq(1L), eq("u:10"))).thenReturn(7L)

        assertThat(service.execute(1L, request(session))).isEqualTo(7L)
    }

    @Test
    @DisplayName("비로그인은 접속 IP와 User-Agent 해시로 구분하고, 같은 입력이면 같은 키다")
    fun keysByHashedClientForAnonymous() {
        whenever(counter.recordView(any(), any())).thenReturn(1L)

        service.execute(1L, request(null))
        service.execute(1L, request(null))
        service.execute(1L, request(null, ip = "10.0.0.2"))

        val keys = argumentCaptor<String>()
        verify(counter, org.mockito.kotlin.times(3)).recordView(any(), keys.capture())
        assertThat(keys.firstValue).startsWith("a:").isEqualTo(keys.secondValue).isNotEqualTo(keys.thirdValue)
        assertThat(keys.firstValue).doesNotContain("10.0.0.1")
    }

    @Test
    @DisplayName("집계(Redis)가 실패해도 예외 없이 null을 돌려준다")
    fun swallowsCounterFailure() {
        whenever(counter.recordView(any(), any())).thenThrow(RuntimeException("redis down"))

        assertThat(service.execute(1L, request(null))).isNull()
    }
}
