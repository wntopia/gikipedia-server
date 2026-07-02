package io.github.wntopia.gikipedia.server.domain.auth.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;

public interface QueryAuthorizationUriService {

  ResponseEntity<Void> execute(HttpSession session);
}
