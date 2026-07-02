package io.github.wntopia.gikipedia.server.domain.auth.service;

import jakarta.servlet.http.HttpSession;

public interface SignoutService {

  void execute(HttpSession session);
}
