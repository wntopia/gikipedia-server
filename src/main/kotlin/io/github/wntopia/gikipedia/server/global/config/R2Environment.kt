package io.github.wntopia.gikipedia.server.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "spring.cloud.cloudflare")
data class R2Environment(
    val bucket: String? = null,
    val endpoint: String? = null,
    val accessKey: String? = null,
    val secretKey: String? = null,
    val publicUrl: String? = null,
)
