package io.github.wntopia.gikipedia.server.domain.auth.service;

import io.github.wntopia.gikipedia.server.domain.auth.dto.response.AuthStatusResDto;
import jakarta.servlet.http.HttpSession;

public interface QueryAuthStatusService {

  AuthStatusResDto execute(HttpSession session);
}
