package io.github.wntopia.gikipedia.server.domain.auth.service;

import io.github.wntopia.gikipedia.server.domain.auth.dto.request.DataGsmOAuthCallbackReqDto;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;

public interface SigninService {

  ResponseEntity<Void> execute(DataGsmOAuthCallbackReqDto reqDto, HttpSession session);
}
