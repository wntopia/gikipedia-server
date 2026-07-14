package io.github.wntopia.gikipedia.server.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.util.StringUtils
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import java.net.URI

@Configuration
class R2Config(
    private val environment: R2Environment,
) {
    @Bean(destroyMethod = "close")
    fun s3Client(): S3Client =
        S3Client
            .builder()
            .endpointOverride(URI.create(requireProperty(environment.endpoint, "spring.cloud.cloudflare.endpoint")))
            .region(Region.of("auto"))
            .forcePathStyle(true)
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                        requireProperty(environment.accessKey, "spring.cloud.cloudflare.access-key"),
                        requireProperty(environment.secretKey, "spring.cloud.cloudflare.secret-key"),
                    ),
                ),
            ).build()

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
