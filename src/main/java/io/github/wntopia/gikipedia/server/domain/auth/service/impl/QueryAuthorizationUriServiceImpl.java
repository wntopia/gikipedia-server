package io.github.wntopia.gikipedia.server.domain.auth.service.impl;

import io.github.wntopia.gikipedia.server.domain.auth.service.QueryAuthorizationUriService;
import io.github.wntopia.gikipedia.server.global.config.DataGsmOAuthEnvironment;
import io.github.wntopia.gikipedia.server.global.security.session.OAuthSessionAttributes;
import jakarta.servlet.http.HttpSession;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import team.themoment.datagsm.sdk.oauth.DataGsmOAuthClient;
import team.themoment.datagsm.sdk.oauth.model.AuthorizationUrlBuilder;

@Service
@RequiredArgsConstructor
public class QueryAuthorizationUriServiceImpl implements QueryAuthorizationUriService {
  private final DataGsmOAuthEnvironment environment;
  private final DataGsmOAuthClient client;

  @Override
  public ResponseEntity<Void> execute(HttpSession session) {
    return redirect(createAuthorizationRedirect(session));
  }

  private URI createAuthorizationRedirect(HttpSession session) {
    String state = UUID.randomUUID().toString();
    AuthorizationUrlBuilder builder =
        client.createAuthorizationUrl(environment.redirectUri()).state(state).enablePkce();

    if (StringUtils.hasText(environment.scope())) {
      builder.scope(environment.scope());
    }

    session.setAttribute(OAuthSessionAttributes.DATAGSM_STATE, state);
    session.setAttribute(OAuthSessionAttributes.DATAGSM_CODE_VERIFIER, builder.getCodeVerifier());

    return URI.create(builder.build());
  }

  private ResponseEntity<Void> redirect(URI uri) {
    return ResponseEntity.status(HttpStatus.FOUND)
        .header(HttpHeaders.LOCATION, uri.toString())
        .build();
  }
}
