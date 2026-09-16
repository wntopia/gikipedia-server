package io.github.wntopia.gikipedia.server.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "seaweedfs")
data class SeaweedEnvironment(
    val filerUrl: String? = null,
    val publicUrl: String? = null,
)
