package io.github.wntopia.gikipedia.server.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "spring.security.cors")
data class CorsEnvironment(
    val allowedOrigins: List<String> = emptyList(),
)
