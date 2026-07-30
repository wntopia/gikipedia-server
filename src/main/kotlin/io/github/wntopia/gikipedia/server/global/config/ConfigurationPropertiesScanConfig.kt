package io.github.wntopia.gikipedia.server.global.config

import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.annotation.Configuration

/**
 * 모든 @ConfigurationProperties 클래스가 이 config 패키지 안에 있으므로, 인자 없는
 * @ConfigurationPropertiesScan을 이 패키지의 클래스에 붙이는 것만으로 기존과 동일한 범위가 스캔된다.
 */
@Configuration
@ConfigurationPropertiesScan
class ConfigurationPropertiesScanConfig
