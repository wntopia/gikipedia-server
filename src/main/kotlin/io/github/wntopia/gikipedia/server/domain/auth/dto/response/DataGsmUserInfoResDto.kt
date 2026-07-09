package io.github.wntopia.gikipedia.server.domain.auth.dto.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable

@JsonIgnoreProperties(ignoreUnknown = true)
data class DataGsmUserInfoResDto(
    val id: Long?,
    val email: String?,
    val role: String?,
    @JsonProperty("isStudent") val isStudent: Boolean?,
    val student: DataGsmStudentInfoResDto?,
) : Serializable
