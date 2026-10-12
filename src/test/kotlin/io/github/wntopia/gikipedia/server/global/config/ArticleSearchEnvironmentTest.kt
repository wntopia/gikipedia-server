package io.github.wntopia.gikipedia.server.global.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration
import java.time.Duration

class ArticleSearchEnvironmentTest {
    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ArticleSearchEnvironment::class)
    class TestConfig

    private val runner = ApplicationContextRunner().withUserConfiguration(TestConfig::class.java)

    @Test
    fun `기본 제한 시간은 2초이고 설정으로 바꿀 수 있다`() {
        runner.run { context ->
            assertThat(context).hasNotFailed()
            assertThat(context.getBean(ArticleSearchEnvironment::class.java).maxTime).isEqualTo(Duration.ofSeconds(2))
        }
        runner.withPropertyValues("article.search.max-time=500ms").run { context ->
            assertThat(context).hasNotFailed()
            assertThat(context.getBean(ArticleSearchEnvironment::class.java).maxTime).isEqualTo(Duration.ofMillis(500))
        }
    }

    @Test
    fun `무제한 실행이 되는 0과 음수는 시작 시 거부한다`() {
        for (value in listOf("0s", "-1s", "0.000001s")) {
            runner.withPropertyValues("article.search.max-time=$value").run { context ->
                assertThat(context).hasFailed()
            }
        }
    }
}
