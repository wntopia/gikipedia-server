package io.github.wntopia.gikipedia.server.domain.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DataGsmUserInfoResDto(
    Long id,
    String email,
    String role,
    @JsonProperty("isStudent") Boolean isStudent,
    DataGsmStudentInfoResDto student)
    implements Serializable {}
