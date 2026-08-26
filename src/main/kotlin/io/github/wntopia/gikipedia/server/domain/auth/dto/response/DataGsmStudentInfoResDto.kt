package io.github.wntopia.gikipedia.server.domain.auth.dto.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.io.Serializable

@JsonIgnoreProperties(ignoreUnknown = true)
data class DataGsmStudentInfoResDto(
    val id: Long?,
    val name: String?,
    val sex: String?,
    val email: String?,
    val grade: Int?,
    val classNum: Int?,
    val number: Int?,
    val studentNumber: Int?,
    val major: String?,
    val specialty: String?,
    val role: String?,
    val dormitoryFloor: Int?,
    val dormitoryRoom: Int?,
    val isLeaveSchool: Boolean?,
    val majorClub: DataGsmClubSummaryResDto?,
    val autonomousClub: DataGsmClubSummaryResDto?,
    val githubId: String?,
    val githubUrl: String?,
) : Serializable
