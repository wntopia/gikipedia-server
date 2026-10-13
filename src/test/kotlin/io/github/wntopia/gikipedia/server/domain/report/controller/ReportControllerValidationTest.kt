package io.github.wntopia.gikipedia.server.domain.report.controller

import io.github.wntopia.gikipedia.server.domain.report.dto.response.ReportPageResDto
import io.github.wntopia.gikipedia.server.domain.report.service.QueryReportsService
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
class ReportControllerValidationTest {
    private val queryReportsService = mock<QueryReportsService>()
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        val validator = LocalValidatorFactoryBean().apply { afterPropertiesSet() }
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(ReportController(mock(), queryReportsService, mock()))
                .setValidator(validator)
                .build()
        whenever(queryReportsService.execute(any(), any(), any(), any()))
            .thenReturn(ReportPageResDto(emptyList(), 0, 20, 0, false))
    }

    @Test
    @DisplayName("page가 음수이거나 size가 범위를 벗어나면 400")
    fun rejectsOutOfRangePaging() {
        mockMvc.perform(get("/api/v1/reports").param("page", "-1")).andExpect(status().isBadRequest)
        mockMvc.perform(get("/api/v1/reports").param("size", "0")).andExpect(status().isBadRequest)
        mockMvc.perform(get("/api/v1/reports").param("size", "101")).andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("기본값과 상태 필터는 통과한다")
    fun acceptsDefaultsAndStatusFilter() {
        mockMvc.perform(get("/api/v1/reports")).andExpect(status().isOk)
        mockMvc.perform(get("/api/v1/reports").param("status", "OPEN")).andExpect(status().isOk)
    }
}
