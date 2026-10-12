package io.github.wntopia.gikipedia.server.domain.article.controller

import io.github.wntopia.gikipedia.server.domain.article.dto.response.ArticleResDto
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleSearchDocument
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleSearchRepository
import io.github.wntopia.gikipedia.server.domain.article.service.CreateArticleService
import io.github.wntopia.gikipedia.server.domain.article.service.QueryArticleService
import io.github.wntopia.gikipedia.server.domain.article.service.UpdateArticleImageService
import io.github.wntopia.gikipedia.server.domain.article.service.UpdateArticleService
import io.github.wntopia.gikipedia.server.domain.article.service.impl.SearchArticleServiceImpl
import io.github.wntopia.gikipedia.server.global.security.config.SecurityConfig
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.dao.DataAccessResourceFailureException
import org.springframework.dao.QueryTimeoutException
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import team.themoment.sdk.config.ExceptionProperties
import team.themoment.sdk.config.ResponseProperties
import team.themoment.sdk.exception.GlobalExceptionHandler
import team.themoment.sdk.response.ApiResponseWrapper
import java.time.Instant

@WebMvcTest(ArticleController::class, ArticleSearchController::class)
@Import(
    SearchArticleServiceImpl::class,
    SecurityConfig::class,
    ApiResponseWrapper::class,
    GlobalExceptionHandler::class,
    ArticleSearchControllerTest.Configuration::class,
)
class ArticleSearchControllerTest {
    @TestConfiguration(proxyBeanMethods = false)
    class Configuration {
        @Bean
        fun responseProperties() = ResponseProperties()

        @Bean
        fun exceptionProperties() = ExceptionProperties()

        @Bean
        fun corsConfigurationSource(): CorsConfigurationSource = UrlBasedCorsConfigurationSource()
    }

    @Autowired
    private lateinit var mvc: MockMvc

    @MockitoBean
    private lateinit var repository: ArticleSearchRepository

    @MockitoBean
    private lateinit var createArticleService: CreateArticleService

    @MockitoBean
    private lateinit var queryArticleService: QueryArticleService

    @MockitoBean
    private lateinit var updateArticleService: UpdateArticleService

    @MockitoBean
    private lateinit var updateArticleImageService: UpdateArticleImageService

    @Test
    fun `익명 검색의 기본 페이지와 응답 래퍼를 검증한다`() {
        whenever(repository.count("급식")).thenReturn(21)
        whenever(repository.search("급식", 0, 20)).thenReturn(
            listOf(ArticleSearchDocument(7, "급식 안내", "학교 급식 안내", Instant.parse("2026-10-12T00:00:00Z"))),
        )
        mvc
            .perform(get("/api/v1/articles").param("q", " 급식 "))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.page").value(0))
            .andExpect(jsonPath("$.data.size").value(20))
            .andExpect(jsonPath("$.data.totalElements").value(21))
            .andExpect(jsonPath("$.data.totalPages").value(2))
            .andExpect(jsonPath("$.data.items[0].id").value(7))
            .andExpect(jsonPath("$.data.items[0].snippet[1].text").value("급식"))
            .andExpect(jsonPath("$.data.items[0].snippet[1].highlight").value(true))
            .andExpect(jsonPath("$.data.items[0].updatedAt").value("2026-10-12T00:00:00Z"))
            .andExpect(jsonPath("$.data.data").doesNotExist())
        verify(repository).search("급식", 0, 20)
    }

    @ParameterizedTest
    @ValueSource(strings = ["", " ", "\t", "\u00a0", "a\u0000b"])
    fun `비어 있거나 사용할 수 없는 검색어는 400이다`(query: String) {
        mvc
            .perform(get("/api/v1/articles").param("q", query))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").isString)
            .andExpect(jsonPath("$.code").exists())
        verifyNoInteractions(repository)
    }

    @Test
    fun `검색어 누락과 길이 초과는 400이고 이모지 100자는 허용한다`() {
        mvc
            .perform(get("/api/v1/articles"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").isString)
            .andExpect(jsonPath("$.code").exists())
        mvc
            .perform(get("/api/v1/articles").param("q", "가".repeat(101)))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").isString)
            .andExpect(jsonPath("$.code").exists())
        mvc
            .perform(get("/api/v1/articles").param("q", "🍱".repeat(101)))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").isString)
            .andExpect(jsonPath("$.code").exists())
        verifyNoInteractions(repository)
        whenever(repository.search(any(), any(), any())).thenReturn(emptyList())
        mvc
            .perform(get("/api/v1/articles").param("q", "🍱".repeat(100)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items").isEmpty)
            .andExpect(jsonPath("$.data.totalPages").value(0))
    }

    @ParameterizedTest
    @ValueSource(strings = ["-1", "abc", "2147483648"])
    fun `잘못된 페이지 번호는 400이다`(page: String) {
        mvc
            .perform(get("/api/v1/articles").param("q", "급식").param("page", page))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").isString)
            .andExpect(jsonPath("$.code").exists())
        verifyNoInteractions(repository)
    }

    @ParameterizedTest
    @ValueSource(strings = ["0", "51", "abc", "2147483648"])
    fun `잘못된 페이지 크기는 400이다`(size: String) {
        mvc
            .perform(get("/api/v1/articles").param("q", "급식").param("size", size))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").isString)
            .andExpect(jsonPath("$.code").exists())
        verifyNoInteractions(repository)
    }

    @Test
    fun `페이지 끝을 넘으면 요청 페이지와 전체 건수를 유지한다`() {
        whenever(repository.count("급식")).thenReturn(1)
        whenever(repository.search("급식", 500, 50)).thenReturn(emptyList())
        mvc
            .perform(get("/api/v1/articles").param("q", "급식").param("page", "10").param("size", "50"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items").isEmpty)
            .andExpect(jsonPath("$.data.page").value(10))
            .andExpect(jsonPath("$.data.totalElements").value(1))
    }

    @Test
    fun `MongoDB 장애와 시간 초과는 공통 형식의 503이다`() {
        whenever(repository.count("연결")).thenThrow(DataAccessResourceFailureException("secret host"))
        whenever(repository.count("시간")).thenThrow(QueryTimeoutException("internal query"))
        for (query in listOf("연결", "시간")) {
            mvc
                .perform(get("/api/v1/articles").param("q", query))
                .andExpect(status().isServiceUnavailable)
                .andExpect(jsonPath("$.message").value("문서 검색을 일시적으로 사용할 수 없습니다."))
        }
    }

    @Test
    fun `기존 ID 조회 경로를 유지한다`() {
        whenever(queryArticleService.execute(7)).thenReturn(ArticleResDto(7, "제목", "본문", null, null, null))
        mvc
            .perform(get("/api/v1/articles/7"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.id").value(7))
        verifyNoInteractions(repository)
    }
}
