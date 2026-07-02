package io.github.wntopia.gikipedia.server.global.security.session;

import io.github.wntopia.gikipedia.server.domain.auth.dto.response.DataGsmUserInfoResDto;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationReader {

  public DataGsmUserInfoResDto getCurrentUser(HttpSession session) {
    return (DataGsmUserInfoResDto) session.getAttribute(OAuthSessionAttributes.DATAGSM_USER_INFO);
  }
}
