package io.github.wntopia.gikipedia.server.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import team.themoment.datagsm.sdk.oauth.DataGsmOAuthClient;

@Configuration
public class DataGsmConfig {

  @Bean(destroyMethod = "close")
  DataGsmOAuthClient dataGsmOAuthClient(DataGsmOAuthEnvironment environment) {
    DataGsmOAuthClient.Builder builder =
        DataGsmOAuthClient.builder(
            requireProperty(
                environment.clientId(),
                "spring.security.oauth2.client.registration.datagsm.client-id"),
            requireProperty(
                environment.clientSecret(),
                "spring.security.oauth2.client.registration.datagsm.client-secret"));

    if (StringUtils.hasText(environment.authorizationBaseUrl())) {
      builder.authorizationBaseUrl(stripTrailingSlash(environment.authorizationBaseUrl()));
    }
    if (StringUtils.hasText(environment.resourceBaseUrl())) {
      builder.userInfoBaseUrl(stripTrailingSlash(environment.resourceBaseUrl()));
    }

    return builder.build();
  }

  private static String requireProperty(String value, String propertyName) {
    if (!StringUtils.hasText(value)) {
      throw new IllegalStateException(propertyName + " must be configured");
    }
    return value;
  }

  private static String stripTrailingSlash(String value) {
    return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
  }
}
