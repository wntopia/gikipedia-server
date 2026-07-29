package io.github.wntopia.gikipedia.server.global.config

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.wntopia.gikipedia.server.domain.article.dto.response.ArticleResDto
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer

/**
 * 조회 응답(ArticleResDto)을 Redis에 JSON으로 캐싱하기 위한 RedisTemplate.
 *
 * `spring.session.store-type: redis`(세션 저장)와는 완전히 별개 용도라, 키는 [ArticleCacheStore]가
 * 별도 네임스페이스("article:cache:")를 붙여서 쓴다.
 */
@Configuration
class ArticleCacheRedisConfig(
    private val objectMapper: ObjectMapper,
) {
    @Bean
    fun articleCacheRedisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, ArticleResDto> {
        val template = RedisTemplate<String, ArticleResDto>()
        template.connectionFactory = connectionFactory
        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = Jackson2JsonRedisSerializer(objectMapper, ArticleResDto::class.java)
        template.afterPropertiesSet()
        return template
    }
}
