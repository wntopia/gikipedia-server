package io.github.wntopia.gikipedia.server.domain.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DataGsmStudentInfoResDto(
    Long id,
    String name,
    String sex,
    String email,
    Integer grade,
    Integer classNum,
    Integer number,
    Integer studentNumber,
    String major,
    String specialty,
    String role,
    Integer dormitoryFloor,
    Integer dormitoryRoom,
    Boolean isLeaveSchool,
    DataGsmClubSummaryResDto majorClub,
    DataGsmClubSummaryResDto autonomousClub,
    String githubId,
    String githubUrl)
    implements Serializable {}
