package io.github.wntopia.gikipedia.server.domain.auth.dto.request

import org.springframework.web.bind.annotation.BindParam

data class DataGsmOAuthCallbackReqDto(
    val code: String? = null,
    val state: String? = null,
    val error: String? = null,
    @BindParam("error_description") val errorDescription: String? = null,
)
