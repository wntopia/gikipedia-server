package io.github.wntopia.gikipedia.server.global.exception

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import tools.jackson.module.kotlin.jacksonObjectMapper

class UnhandledExceptionResolverTest {
    private val objectMapper = jacksonObjectMapper()
    private val resolver = UnhandledExceptionResolver(objectMapper)

    @Test
    @DisplayName("처리되지 않은 예외는 빈 200이 아니라 500과 CommonApiResponse 봉투로 응답한다")
    fun writesErrorEnvelope() {
        val response = MockHttpServletResponse()

        val mav =
            resolver.resolveException(
                MockHttpServletRequest("GET", "/api/v1/articles/3"),
                response,
                null,
                IllegalStateException("mongo timeout"),
            )

        assertNotNull(mav)
        assertEquals(500, response.status)
        val body = objectMapper.readTree(response.contentAsString)
        assertEquals(500, body["code"].asInt())
        assertEquals("서버 내부 오류가 발생했습니다.", body["message"].asString())
    }

    @Test
    @DisplayName("이미 응답이 커밋됐으면 건드리지 않는다")
    fun skipsCommittedResponse() {
        val response = MockHttpServletResponse().apply { setCommitted(true) }

        assertNull(resolver.resolveException(MockHttpServletRequest(), response, null, IllegalStateException()))
    }
}
