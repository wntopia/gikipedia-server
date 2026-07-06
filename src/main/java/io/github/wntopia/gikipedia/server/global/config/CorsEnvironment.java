package io.github.wntopia.gikipedia.server.global.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.security.cors")
public record CorsEnvironment(List<String> allowedOrigins) {

  public CorsEnvironment {
    allowedOrigins = allowedOrigins == null ? List.of() : allowedOrigins;
  }
}
