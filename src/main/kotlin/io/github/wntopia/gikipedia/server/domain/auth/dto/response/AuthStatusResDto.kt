package io.github.wntopia.gikipedia.server.domain.auth.dto.response

data class AuthStatusResDto(
    val authenticated: Boolean,
    val user: DataGsmUserInfoResDto?,
)
