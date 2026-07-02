package io.github.wntopia.gikipedia.server.global.config;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.security.oauth2.client")
public record DataGsmOAuthEnvironment(
    Map<String, Registration> registration, Map<String, Provider> provider) {
  private static final String DATAGSM = "datagsm";
  private static final String AUTHORIZATION_PATH = "/v1/oauth/authorize";
  private static final String USER_INFO_PATH = "/userinfo";

  public String authorizationBaseUrl() {
    return removeSuffix(datagsmProvider().authorizationUri(), AUTHORIZATION_PATH);
  }

  public String resourceBaseUrl() {
    return removeSuffix(datagsmProvider().userInfoUri(), USER_INFO_PATH);
  }

  public String clientId() {
    return datagsmRegistration().clientId();
  }

  public String clientSecret() {
    return datagsmRegistration().clientSecret();
  }

  public String redirectUri() {
    return datagsmRegistration().redirectUri();
  }

  public String scope() {
    return datagsmRegistration().scope();
  }

  public String successRedirectUri() {
    return datagsmRegistration().successRedirectUri();
  }

  public String failureRedirectUri() {
    return datagsmRegistration().failureRedirectUri();
  }

  private Registration datagsmRegistration() {
    if (registration == null || registration.get(DATAGSM) == null) {
      throw new IllegalStateException(
          "spring.security.oauth2.client.registration.datagsm must be configured");
    }
    return registration.get(DATAGSM);
  }

  private Provider datagsmProvider() {
    if (provider == null || provider.get(DATAGSM) == null) {
      throw new IllegalStateException(
          "spring.security.oauth2.client.provider.datagsm must be configured");
    }
    return provider.get(DATAGSM);
  }

  private static String removeSuffix(String value, String suffix) {
    if (value == null || !value.endsWith(suffix)) {
      return value;
    }
    return value.substring(0, value.length() - suffix.length());
  }

  public record Registration(
      String clientId,
      String clientSecret,
      String redirectUri,
      String scope,
      String successRedirectUri,
      String failureRedirectUri) {}

  public record Provider(String authorizationUri, String tokenUri, String userInfoUri) {}
}
