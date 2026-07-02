package io.github.wntopia.gikipedia.server.domain.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DataGsmClubSummaryResDto(
    Long id, String name, String type, String status, Integer foundedYear, Integer abolishedYear)
    implements Serializable {}
