package io.github.wntopia.gikipedia.server.domain.auth.service.impl;

import io.github.wntopia.gikipedia.server.domain.auth.service.SignoutService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

@Service
public class SignoutServiceImpl implements SignoutService {

  @Override
  public void execute(HttpSession session) {
    session.invalidate();
  }
}
