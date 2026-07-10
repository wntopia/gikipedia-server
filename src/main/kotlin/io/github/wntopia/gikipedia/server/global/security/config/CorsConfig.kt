package io.github.wntopia.gikipedia.server.global.security.config

import io.github.wntopia.gikipedia.server.global.config.CorsEnvironment
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
class CorsConfig(
    private val corsEnvironment: CorsEnvironment,
) {
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration =
            CorsConfiguration().apply {
                allowedOriginPatterns = corsEnvironment.allowedOrigins
                allowedMethods = ALLOWED_METHODS
                allowedHeaders = listOf(CorsConfiguration.ALL)
                allowCredentials = true
                maxAge = MAX_AGE
            }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }

    companion object {
        private val ALLOWED_METHODS: List<String> = HttpMethod.values().map { it.name() }
        private const val MAX_AGE = 3600L
    }
}
