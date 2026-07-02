package io.github.wntopia.gikipedia.server.domain.auth.service.impl;

import io.github.wntopia.gikipedia.server.domain.auth.dto.internal.DataGsmSession;
import io.github.wntopia.gikipedia.server.domain.auth.dto.internal.DataGsmToken;
import io.github.wntopia.gikipedia.server.domain.auth.dto.request.DataGsmOAuthCallbackReqDto;
import io.github.wntopia.gikipedia.server.domain.auth.dto.response.DataGsmClubSummaryResDto;
import io.github.wntopia.gikipedia.server.domain.auth.dto.response.DataGsmStudentInfoResDto;
import io.github.wntopia.gikipedia.server.domain.auth.dto.response.DataGsmUserInfoResDto;
import io.github.wntopia.gikipedia.server.domain.auth.service.SigninService;
import io.github.wntopia.gikipedia.server.global.config.DataGsmOAuthEnvironment;
import io.github.wntopia.gikipedia.server.global.security.session.OAuthSessionAttributes;
import jakarta.servlet.http.HttpSession;
import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import team.themoment.datagsm.sdk.oauth.DataGsmOAuthClient;
import team.themoment.datagsm.sdk.oauth.exception.DataGsmException;
import team.themoment.datagsm.sdk.oauth.model.ClubInfo;
import team.themoment.datagsm.sdk.oauth.model.Student;
import team.themoment.datagsm.sdk.oauth.model.TokenResponse;
import team.themoment.datagsm.sdk.oauth.model.UserInfo;
import team.themoment.sdk.exception.ExpectedException;

@Service
@RequiredArgsConstructor
public class SigninServiceImpl implements SigninService {
  private final DataGsmOAuthEnvironment environment;
  private final DataGsmOAuthClient client;

  @Override
  public ResponseEntity<Void> execute(DataGsmOAuthCallbackReqDto reqDto, HttpSession session) {
    if (reqDto.error() != null) {
      return redirect(
          failureRedirect(
              reqDto.errorDescription() != null ? reqDto.errorDescription() : reqDto.error()));
    }

    try {
      DataGsmUserInfoResDto userInfo = authenticate(reqDto.code(), reqDto.state(), session);
      return redirect(successRedirect(userInfo));
    } catch (ExpectedException exception) {
      return redirect(failureRedirect(exception.getMessage()));
    }
  }

  private DataGsmUserInfoResDto authenticate(String code, String state, HttpSession session) {
    String savedState = (String) session.getAttribute(OAuthSessionAttributes.DATAGSM_STATE);
    String codeVerifier =
        (String) session.getAttribute(OAuthSessionAttributes.DATAGSM_CODE_VERIFIER);

    session.removeAttribute(OAuthSessionAttributes.DATAGSM_STATE);
    session.removeAttribute(OAuthSessionAttributes.DATAGSM_CODE_VERIFIER);

    if (!StringUtils.hasText(code)) {
      throw new ExpectedException("인가 코드가 없습니다.", HttpStatus.BAD_REQUEST);
    }
    if (!StringUtils.hasText(savedState) || !Objects.equals(savedState, state)) {
      throw new ExpectedException("OAuth state가 올바르지 않습니다.", HttpStatus.BAD_REQUEST);
    }
    if (!StringUtils.hasText(codeVerifier)) {
      throw new ExpectedException("PKCE code verifier가 없습니다.", HttpStatus.BAD_REQUEST);
    }

    DataGsmToken token;
    DataGsmUserInfoResDto userInfo;
    try {
      TokenResponse sdkToken =
          client.exchangeCodeForToken(code, environment.redirectUri(), codeVerifier);
      UserInfo sdkUserInfo = client.getUserInfo(sdkToken.getAccessToken());
      token = toDataGsmToken(sdkToken);
      userInfo = toDataGsmUserInfoResDto(sdkUserInfo);
    } catch (DataGsmException exception) {
      throw new ExpectedException(
          "DataGSM OAuth 처리에 실패했습니다. " + exception.getMessage(), HttpStatus.BAD_GATEWAY);
    }

    DataGsmSession dataGsmSession = new DataGsmSession(userInfo, token, Instant.now());

    session.setAttribute(OAuthSessionAttributes.DATAGSM_USER_INFO, userInfo);
    session.setAttribute(OAuthSessionAttributes.DATAGSM_TOKEN, token);
    session.setAttribute(OAuthSessionAttributes.DATAGSM_SESSION, dataGsmSession);

    return userInfo;
  }

  private ResponseEntity<Void> redirect(URI uri) {
    return ResponseEntity.status(HttpStatus.FOUND)
        .header(HttpHeaders.LOCATION, uri.toString())
        .build();
  }

  private URI successRedirect(DataGsmUserInfoResDto userInfo) {
    return UriComponentsBuilder.fromUriString(environment.successRedirectUri())
        .queryParam("authenticated", "true")
        .queryParam("student", Boolean.TRUE.equals(userInfo.isStudent()))
        .build()
        .encode()
        .toUri();
  }

  private URI failureRedirect(String reason) {
    return UriComponentsBuilder.fromUriString(environment.failureRedirectUri())
        .queryParam("reason", reason)
        .build()
        .encode()
        .toUri();
  }

  private DataGsmToken toDataGsmToken(TokenResponse token) {
    return new DataGsmToken(
        token.getAccessToken(),
        token.getTokenType(),
        token.getExpiresIn(),
        token.getRefreshToken(),
        token.getScope());
  }

  private DataGsmUserInfoResDto toDataGsmUserInfoResDto(UserInfo userInfo) {
    return new DataGsmUserInfoResDto(
        userInfo.getId(),
        userInfo.getEmail(),
        enumName(userInfo.getRole()),
        userInfo.getIsStudent(),
        toDataGsmStudentInfoResDto(userInfo.getStudent()));
  }

  private DataGsmStudentInfoResDto toDataGsmStudentInfoResDto(Student student) {
    if (student == null) {
      return null;
    }

    return new DataGsmStudentInfoResDto(
        student.getId(),
        student.getName(),
        enumName(student.getSex()),
        student.getEmail(),
        student.getGrade(),
        student.getClassNum(),
        student.getNumber(),
        student.getStudentNumber(),
        enumName(student.getMajor()),
        student.getSpecialty(),
        enumName(student.getRole()),
        student.getDormitoryFloor(),
        student.getDormitoryRoom(),
        student.getIsLeaveSchool(),
        toDataGsmClubSummaryResDto(student.getMajorClub()),
        toDataGsmClubSummaryResDto(student.getAutonomousClub()),
        student.getGithubId(),
        student.getGithubUrl());
  }

  private DataGsmClubSummaryResDto toDataGsmClubSummaryResDto(ClubInfo clubInfo) {
    if (clubInfo == null) {
      return null;
    }

    return new DataGsmClubSummaryResDto(
        clubInfo.getId(),
        clubInfo.getName(),
        enumName(clubInfo.getType()),
        enumName(clubInfo.getStatus()),
        clubInfo.getFoundedYear(),
        clubInfo.getAbolishedYear());
  }

  private String enumName(Enum<?> value) {
    return value != null ? value.name() : null;
  }
}
