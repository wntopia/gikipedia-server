package io.github.wntopia.gikipedia.server.domain.report.controller

import io.github.wntopia.gikipedia.server.domain.report.service.QueryReportsService
import io.github.wntopia.gikipedia.server.global.dto.PageResDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean

/** 잘못된 신고 사유와 페이지 값이 500이 아니라 400으로 거절되는지 확인한다. */
class ReportControllerValidationTest {
    private val queryReportsService = mock<QueryReportsService>()
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        val validator = LocalValidatorFactoryBean().apply { afterPropertiesSet() }
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(ReportController(mock(), queryReportsService))
                .setValidator(validator)
                .build()
        whenever(queryReportsService.execute(any(), any(), any())).thenReturn(PageResDto(emptyList(), 0, 20, 0, false))
    }

    private fun report(reason: String) =
        mockMvc.perform(
            post("/api/v1/articles/1/revisions/1/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"reason":"$reason"}"""),
        )

    @Test
    @DisplayName("사유가 비었거나 500자를 넘으면 400, 정상 사유는 통과한다")
    fun validatesReason() {
        report("  ").andExpect(status().isBadRequest)
        report("가".repeat(501)).andExpect(status().isBadRequest)
        // 201 변환은 standalone에 없는 the-sdk ApiResponseWrapper가 하므로 여기서는 2xx만 확인한다.
        report("비방 문구").andExpect(status().is2xxSuccessful)
    }

    @Test
    @DisplayName("신고 목록의 page/size가 범위를 벗어나면 400")
    fun rejectsOutOfRangePaging() {
        mockMvc.perform(get("/api/v1/reports").param("page", "-1")).andExpect(status().isBadRequest)
        mockMvc.perform(get("/api/v1/reports").param("size", "101")).andExpect(status().isBadRequest)
        mockMvc.perform(get("/api/v1/reports")).andExpect(status().isOk)
    }
}
