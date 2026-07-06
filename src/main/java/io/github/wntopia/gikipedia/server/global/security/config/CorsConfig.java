package io.github.wntopia.gikipedia.server.global.security.config;

import io.github.wntopia.gikipedia.server.global.config.CorsEnvironment;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@RequiredArgsConstructor
public class CorsConfig {

  private static final List<String> ALLOWED_METHODS =
      Arrays.stream(HttpMethod.values()).map(HttpMethod::name).toList();
  private static final long MAX_AGE = 3600L;

  private final CorsEnvironment corsEnvironment;

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOriginPatterns(corsEnvironment.allowedOrigins());
    configuration.setAllowedMethods(ALLOWED_METHODS);
    configuration.setAllowedHeaders(List.of(CorsConfiguration.ALL));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(MAX_AGE);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
