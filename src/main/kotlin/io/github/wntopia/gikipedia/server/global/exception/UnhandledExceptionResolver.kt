package io.github.wntopia.gikipedia.server.global.exception

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerExceptionResolver
import org.springframework.web.servlet.ModelAndView
import team.themoment.sdk.exception.ExpectedException
import team.themoment.sdk.response.CommonApiResponse
import tools.jackson.databind.ObjectMapper

/**
 * 어떤 핸들러도 처리하지 않은 예외를 500 + [CommonApiResponse] 봉투로 응답한다.
 *
 * the-sdk의 GlobalExceptionHandler는 [ExpectedException]만 처리하고, 나머지 예외는 the-sdk LoggingFilter가 로그만
 * 남기고 삼켜 빈 본문의 200이 나간다(#46). `@ExceptionHandler(Exception::class)` advice로 막으면 advice 순서에 따라
 * ExpectedException까지 가로챌 수 있어, 기본 리졸버들(advice → @ResponseStatus → Spring 표준 예외)이 모두 지나친
 * 예외만 받도록 가장 낮은 우선순위의 리졸버로 둔다.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
class UnhandledExceptionResolver(
    private val objectMapper: ObjectMapper,
) : HandlerExceptionResolver {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun resolveException(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any?,
        ex: Exception,
    ): ModelAndView? {
        if (response.isCommitted) return null

        log.error("처리되지 않은 예외 ({} {})", request.method, request.requestURI, ex)
        response.status = HttpStatus.INTERNAL_SERVER_ERROR.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        objectMapper.writeValue(
            response.outputStream,
            CommonApiResponse.error("서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
        )
        return ModelAndView()
    }
}
