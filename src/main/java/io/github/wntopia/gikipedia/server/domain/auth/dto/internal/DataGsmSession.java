package io.github.wntopia.gikipedia.server.domain.auth.dto.internal;

import io.github.wntopia.gikipedia.server.domain.auth.dto.response.DataGsmUserInfoResDto;
import java.io.Serializable;
import java.time.Instant;

public record DataGsmSession(
    DataGsmUserInfoResDto userInfo, DataGsmToken token, Instant authenticatedAt)
    implements Serializable {}
