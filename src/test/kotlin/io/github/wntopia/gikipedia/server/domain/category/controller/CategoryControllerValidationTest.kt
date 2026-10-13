package io.github.wntopia.gikipedia.server.domain.category.controller

import io.github.wntopia.gikipedia.server.domain.category.service.QueryCategoryArticlesService
import io.github.wntopia.gikipedia.server.global.dto.PageResDto
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
class CategoryControllerValidationTest {
    private val articlesService = mock<QueryCategoryArticlesService>()
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        val validator = LocalValidatorFactoryBean().apply { afterPropertiesSet() }
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(CategoryController(mock(), mock(), articlesService))
                .setValidator(validator)
                .build()
        whenever(articlesService.execute(any(), any(), any())).thenReturn(PageResDto(emptyList(), 0, 20, 0, false))
    }

    @Test
    @DisplayName("page가 음수이거나 size가 범위를 벗어나면 400")
    fun rejectsOutOfRangePaging() {
        mockMvc.perform(get("/api/v1/categories/1/articles").param("page", "-1")).andExpect(status().isBadRequest)
        mockMvc.perform(get("/api/v1/categories/1/articles").param("size", "101")).andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("기본값은 통과한다")
    fun acceptsDefaults() {
        mockMvc.perform(get("/api/v1/categories/1/articles")).andExpect(status().isOk)
    }
}
