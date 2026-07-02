package io.github.wntopia.gikipedia.server.domain.auth.dto.request;

import org.springframework.web.bind.annotation.BindParam;

public record DataGsmOAuthCallbackReqDto(
    String code,
    String state,
    String error,
    @BindParam("error_description") String errorDescription) {}
