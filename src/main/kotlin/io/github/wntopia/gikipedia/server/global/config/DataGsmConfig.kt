package io.github.wntopia.gikipedia.server.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.util.StringUtils
import team.themoment.datagsm.sdk.oauth.DataGsmOAuthClient

@Configuration
class DataGsmConfig {
    @Bean(destroyMethod = "close")
    fun dataGsmOAuthClient(environment: DataGsmOAuthEnvironment): DataGsmOAuthClient {
        val builder =
            DataGsmOAuthClient.builder(
                requireProperty(
                    environment.clientId(),
                    "spring.security.oauth2.client.registration.datagsm.client-id",
                ),
                requireProperty(
                    environment.clientSecret(),
                    "spring.security.oauth2.client.registration.datagsm.client-secret",
                ),
            )

        if (StringUtils.hasText(environment.authorizationBaseUrl())) {
            builder.authorizationBaseUrl(stripTrailingSlash(environment.authorizationBaseUrl()!!))
        }
        if (StringUtils.hasText(environment.resourceBaseUrl())) {
            builder.userInfoBaseUrl(stripTrailingSlash(environment.resourceBaseUrl()!!))
        }

        return builder.build()
    }

    private fun requireProperty(
        value: String?,
        propertyName: String,
    ): String {
        if (!StringUtils.hasText(value)) {
            throw IllegalStateException("$propertyName must be configured")
        }
        return value!!
    }

    private fun stripTrailingSlash(value: String): String =
        if (value.endsWith("/")) value.substring(0, value.length - 1) else value
}
