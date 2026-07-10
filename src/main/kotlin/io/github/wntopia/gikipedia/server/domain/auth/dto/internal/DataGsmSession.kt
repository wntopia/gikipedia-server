package io.github.wntopia.gikipedia.server.domain.auth.dto.internal

import io.github.wntopia.gikipedia.server.domain.auth.dto.response.DataGsmUserInfoResDto
import java.io.Serializable
import java.time.Instant

data class DataGsmSession(
    val userInfo: DataGsmUserInfoResDto,
    val token: DataGsmToken,
    val authenticatedAt: Instant,
) : Serializable
