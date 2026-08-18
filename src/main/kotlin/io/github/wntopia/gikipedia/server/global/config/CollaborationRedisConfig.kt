package io.github.wntopia.gikipedia.server.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer

/**
 * 편집 중인 CRDT(Yjs) 문서의 바이너리 상태를 저장하기 위한 RedisTemplate.
 *
 * `spring.session.store-type: redis`(세션)나 `article:cache:`(조회 캐시)와는 완전히 별개 용도라, 키는
 * [io.github.wntopia.gikipedia.server.domain.collaboration.service.CollaborationCrdtStateStore]가 별도
 * 네임스페이스("collab:crdt-state:")를 붙여서 쓴다. 값은 opaque 바이트라 JSON 변환 없이 그대로 저장한다.
 */
@Configuration
class CollaborationRedisConfig {
    @Bean
    fun collaborationCrdtRedisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, ByteArray> {
        val template = RedisTemplate<String, ByteArray>()
        template.connectionFactory = connectionFactory
        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = RedisSerializer.byteArray()
        template.afterPropertiesSet()
        return template
    }
}
