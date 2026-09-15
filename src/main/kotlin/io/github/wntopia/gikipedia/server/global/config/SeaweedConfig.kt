package io.github.wntopia.gikipedia.server.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.util.StringUtils
import org.springframework.web.client.RestClient

@Configuration
class SeaweedConfig(
    private val environment: SeaweedEnvironment,
) {
    @Bean
    fun seaweedFilerClient(): RestClient =
        RestClient
            .builder()
            .baseUrl(requireProperty(environment.filerUrl, "seaweedfs.filer-url"))
            .build()

    private fun requireProperty(
        value: String?,
        propertyName: String,
    ): String {
        if (!StringUtils.hasText(value)) {
            throw IllegalStateException("$propertyName must be configured")
        }
        return value!!
    }
}
