package io.github.wntopia.gikipedia.server.domain.article.service

import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.global.config.ArticleViewEnvironment
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.redis.core.HashOperations
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration

class ArticleViewCounterTest {
    private val redis = mock<StringRedisTemplate>()
    private val values = mock<ValueOperations<String, String>>()
    private val hash = mock<HashOperations<String, String, String>>()
    private val articleRepository = mock<ArticleRepository>()
    private val counter = ArticleViewCounter(redis, articleRepository, ArticleViewEnvironment(dedupeMinutes = 30))

    @BeforeEach
    fun setUp() {
        whenever(redis.opsForValue()).thenReturn(values)
        whenever(redis.opsForHash<String, String>()).thenReturn(hash)
    }

    @Test
    @DisplayName("처음 보는 조회자의 조회는 누적과 미반영 증가분을 1씩 올리고 새 누적 값을 돌려준다")
    fun countsFirstView() {
        whenever(hash.get("article:views:total", "1")).thenReturn("5")
        whenever(
            values.setIfAbsent(eq("article:views:seen:1:u:10"), eq("1"), eq(Duration.ofMinutes(30))),
        ).thenReturn(true)
        whenever(hash.increment("article:views:total", "1", 1)).thenReturn(6)

        assertThat(counter.recordView(1L, "u:10")).isEqualTo(6)
        verify(hash).increment("article:views:pending", "1", 1)
    }

    @Test
    @DisplayName("중복 집계 시간 안의 재조회는 올리지 않고 현재 값만 돌려준다")
    fun ignoresRepeatView() {
        whenever(hash.get("article:views:total", "1")).thenReturn("5")
        whenever(values.setIfAbsent(any(), any(), any<Duration>())).thenReturn(false)

        assertThat(counter.recordView(1L, "u:10")).isEqualTo(5)
        verify(hash, never()).increment(any(), any(), any<Long>())
    }

    @Test
    @DisplayName("Redis에 누적 값이 없으면 MySQL 값과 미반영 증가분을 합쳐 채운다")
    fun seedsTotalFromDatabase() {
        whenever(hash.get("article:views:total", "1")).thenReturn(null, "107")
        whenever(hash.get("article:views:pending", "1")).thenReturn("7")
        whenever(articleRepository.findViewCountById(1L)).thenReturn(100L)
        whenever(values.setIfAbsent(any(), any(), any<Duration>())).thenReturn(false)

        assertThat(counter.recordView(1L, "u:10")).isEqualTo(107)
        verify(hash).putIfAbsent("article:views:total", "1", "107")
    }

    @Test
    @DisplayName("미반영 증가분이 없으면 비어 있는 결과를 돌려주고, 있으면 processing으로 옮겨 읽는다")
    fun drainsPending() {
        whenever(redis.hasKey("article:views:processing")).thenReturn(false)
        whenever(redis.hasKey("article:views:pending")).thenReturn(false)
        assertThat(counter.drainPending()).isEmpty()

        whenever(redis.hasKey("article:views:pending")).thenReturn(true)
        whenever(hash.entries("article:views:processing")).thenReturn(mapOf("1" to "3", "2" to "4"))
        assertThat(counter.drainPending()).containsExactlyInAnyOrderEntriesOf(mapOf(1L to 3L, 2L to 4L))
        verify(redis).rename("article:views:pending", "article:views:processing")
    }

    @Test
    @DisplayName("이전에 반영하다 만 processing이 남아 있으면 그것부터 다시 읽는다")
    fun resumesLeftoverProcessing() {
        whenever(redis.hasKey("article:views:processing")).thenReturn(true)
        whenever(hash.entries("article:views:processing")).thenReturn(mapOf("1" to "3"))

        assertThat(counter.drainPending()).containsExactlyEntriesOf(mapOf(1L to 3L))
        verify(redis, never()).rename(any(), any())
    }
}
