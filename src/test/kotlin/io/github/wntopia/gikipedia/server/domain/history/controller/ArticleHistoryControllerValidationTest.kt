package io.github.wntopia.gikipedia.server.domain.history.controller

import io.github.wntopia.gikipedia.server.domain.history.service.ReconstructArticleService
import io.github.wntopia.gikipedia.server.global.dto.PageResDto
import io.github.wntopia.gikipedia.server.global.security.session.AuthenticationReader
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean

/** 범위를 벗어난 `page`/`size`가 500이 아니라 400으로 거절되는지 확인한다. */
class ArticleHistoryControllerValidationTest {
    private val reconstructService = mock<ReconstructArticleService>()
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        val validator = LocalValidatorFactoryBean().apply { afterPropertiesSet() }
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(
                    ArticleHistoryController(reconstructService, mock(), mock<AuthenticationReader>()),
                ).setValidator(validator)
                .build()
        whenever(reconstructService.listRevisions(any(), any(), any()))
            .thenReturn(PageResDto(emptyList(), 0, 20, 0, false))
    }

    @Test
    @DisplayName("page가 음수이거나 size가 범위를 벗어나면 400")
    fun rejectsOutOfRangePaging() {
        mockMvc.perform(get("/api/v1/articles/1/revisions").param("page", "-1")).andExpect(status().isBadRequest)
        mockMvc.perform(get("/api/v1/articles/1/revisions").param("size", "0")).andExpect(status().isBadRequest)
        mockMvc.perform(get("/api/v1/articles/1/revisions").param("size", "101")).andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("기본값과 범위 안의 값은 통과한다")
    fun acceptsDefaultsAndValidValues() {
        mockMvc.perform(get("/api/v1/articles/1/revisions")).andExpect(status().isOk)
        mockMvc
            .perform(
                get("/api/v1/articles/1/revisions").param("page", "2").param("size", "50"),
            ).andExpect(status().isOk)
    }
}
