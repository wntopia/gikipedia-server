package io.github.wntopia.gikipedia.server.domain.auth.dto.response;

public record AuthStatusResDto(boolean authenticated, DataGsmUserInfoResDto user) {}
