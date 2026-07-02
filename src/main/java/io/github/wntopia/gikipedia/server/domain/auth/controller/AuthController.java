package io.github.wntopia.gikipedia.server.domain.auth.controller;

import io.github.wntopia.gikipedia.server.domain.auth.dto.request.DataGsmOAuthCallbackReqDto;
import io.github.wntopia.gikipedia.server.domain.auth.dto.response.AuthStatusResDto;
import io.github.wntopia.gikipedia.server.domain.auth.service.QueryAuthStatusService;
import io.github.wntopia.gikipedia.server.domain.auth.service.QueryAuthorizationUriService;
import io.github.wntopia.gikipedia.server.domain.auth.service.SigninService;
import io.github.wntopia.gikipedia.server.domain.auth.service.SignoutService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import team.themoment.sdk.response.CommonApiResponse;

@RestController
@RequiredArgsConstructor
public class AuthController {
  private final QueryAuthorizationUriService queryAuthorizationUriService;
  private final SigninService signinService;
  private final QueryAuthStatusService queryAuthStatusService;
  private final SignoutService signoutService;

  @GetMapping("/api/v1/oauth/datagsm/signin")
  public ResponseEntity<Void> signin(HttpSession session) {
    return queryAuthorizationUriService.execute(session);
  }

  @GetMapping("/api/v1/oauth/datagsm/callback")
  public ResponseEntity<Void> callback(
      @ModelAttribute DataGsmOAuthCallbackReqDto reqDto, HttpSession session) {
    return signinService.execute(reqDto, session);
  }

  @GetMapping("/api/v1/auth/me")
  public AuthStatusResDto me(HttpSession session) {
    return queryAuthStatusService.execute(session);
  }

  @PostMapping("/api/v1/auth/signout")
  public CommonApiResponse<?> signout(HttpSession session) {
    signoutService.execute(session);
    return CommonApiResponse.success("로그아웃되었습니다.");
  }
}
