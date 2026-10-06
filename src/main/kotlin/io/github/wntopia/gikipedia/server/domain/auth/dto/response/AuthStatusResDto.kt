package io.github.wntopia.gikipedia.server.domain.auth.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class AuthStatusResDto(
    @field:Schema(description = "로그인 여부 (이 API가 200이면 항상 true)")
    val authenticated: Boolean,
    @field:Schema(description = "DataGSM 사용자 정보")
    val user: DataGsmUserInfoResDto?,
)
