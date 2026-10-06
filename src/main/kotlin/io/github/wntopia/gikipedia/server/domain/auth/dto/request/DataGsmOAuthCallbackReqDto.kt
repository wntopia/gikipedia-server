package io.github.wntopia.gikipedia.server.domain.auth.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.web.bind.annotation.BindParam

data class DataGsmOAuthCallbackReqDto(
    @field:Schema(description = "DataGSM이 발급한 인가 코드")
    val code: String? = null,
    @field:Schema(description = "로그인 시작 시 서버가 발급한 CSRF 방지용 state")
    val state: String? = null,
    @field:Schema(description = "인가 실패 시 DataGSM이 전달하는 에러 코드")
    val error: String? = null,
    @field:Schema(description = "인가 실패 시 DataGSM이 전달하는 에러 설명 (쿼리 파라미터명: error_description)")
    @BindParam("error_description") val errorDescription: String? = null,
)
