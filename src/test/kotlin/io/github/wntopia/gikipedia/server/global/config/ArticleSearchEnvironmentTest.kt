package io.github.wntopia.gikipedia.server.global.config

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.Duration

class ArticleSearchEnvironmentTest {
    @Nested
    @DisplayName("Describe: ArticleSearchEnvironment 생성")
    inner class DescribeConstruction {
        @Nested
        @DisplayName("Context: 설정값을 전달하지 않으면")
        inner class ContextDefaults {
            @Test
            @DisplayName("It: 명령별 제한 시간은 2초다")
            fun usesTwoSecondTimeout() {
                // When
                val environment = ArticleSearchEnvironment()

                // Then
                assertThat(environment.maxTime).isEqualTo(Duration.ofSeconds(2))
            }

            @Test
            @DisplayName("It: 동시 검색 상한은 4개다")
            fun usesFourConcurrentRequests() {
                // When
                val environment = ArticleSearchEnvironment()

                // Then
                assertThat(environment.maxConcurrentRequests).isEqualTo(4)
            }
        }

        @Nested
        @DisplayName("Context: 유효한 설정값을 전달하면")
        inner class ContextCustomSettings {
            @Test
            @DisplayName("It: 전달한 제한 시간과 동시 실행 상한을 사용한다")
            fun usesCustomLimits() {
                // Given
                val timeout = Duration.ofMillis(500)

                // When
                val environment = ArticleSearchEnvironment(timeout, 1)

                // Then
                assertThat(environment.maxTime).isEqualTo(timeout)
                assertThat(environment.maxConcurrentRequests).isEqualTo(1)
            }
        }

        @Nested
        @DisplayName("Context: 제한 시간을 밀리초로 변환한 값이 양수가 아니면")
        inner class ContextInvalidTimeout {
            @ParameterizedTest(name = "{0}ns")
            @ValueSource(longs = [0, -1_000_000_000, 1_000])
            @DisplayName("It: 설정 객체 생성을 거부한다")
            fun rejectsInvalidTimeout(nanos: Long) {
                // Given
                val timeout = Duration.ofNanos(nanos)

                // When / Then
                assertThatThrownBy { ArticleSearchEnvironment(maxTime = timeout) }
                    .isInstanceOf(IllegalArgumentException::class.java)
                    .hasMessage("article.search.max-time은 1ms 이상이어야 합니다.")
            }
        }

        @Nested
        @DisplayName("Context: 동시 실행 상한이 양수가 아니면")
        inner class ContextInvalidConcurrency {
            @ParameterizedTest(name = "상한 {0}")
            @ValueSource(ints = [0, -1])
            @DisplayName("It: 설정 객체 생성을 거부한다")
            fun rejectsInvalidConcurrency(limit: Int) {
                // When / Then
                assertThatThrownBy { ArticleSearchEnvironment(maxConcurrentRequests = limit) }
                    .isInstanceOf(IllegalArgumentException::class.java)
                    .hasMessage("article.search.max-concurrent-requests는 1 이상이어야 합니다.")
            }
        }
    }
}
