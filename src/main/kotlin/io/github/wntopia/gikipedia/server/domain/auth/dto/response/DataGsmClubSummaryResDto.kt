package io.github.wntopia.gikipedia.server.domain.auth.dto.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.io.Serializable

@JsonIgnoreProperties(ignoreUnknown = true)
data class DataGsmClubSummaryResDto(
    val id: Long?,
    val name: String?,
    val type: String?,
    val status: String?,
    val foundedYear: Int?,
    val abolishedYear: Int?,
) : Serializable
