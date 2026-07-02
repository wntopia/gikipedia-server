package io.github.wntopia.gikipedia.server.domain.auth.service.impl;

import io.github.wntopia.gikipedia.server.domain.auth.dto.response.AuthStatusResDto;
import io.github.wntopia.gikipedia.server.domain.auth.dto.response.DataGsmUserInfoResDto;
import io.github.wntopia.gikipedia.server.domain.auth.service.QueryAuthStatusService;
import io.github.wntopia.gikipedia.server.global.security.session.AuthenticationReader;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import team.themoment.sdk.exception.ExpectedException;

@Service
@RequiredArgsConstructor
public class QueryAuthStatusServiceImpl implements QueryAuthStatusService {
  private final AuthenticationReader authenticationReader;

  @Override
  public AuthStatusResDto execute(HttpSession session) {
    DataGsmUserInfoResDto userInfo = authenticationReader.getCurrentUser(session);
    if (userInfo == null) {
      throw new ExpectedException("인증이 필요합니다.", HttpStatus.UNAUTHORIZED);
    }
    return new AuthStatusResDto(true, userInfo);
  }
}
