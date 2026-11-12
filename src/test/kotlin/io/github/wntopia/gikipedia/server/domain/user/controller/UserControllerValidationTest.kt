package io.github.wntopia.gikipedia.server.domain.user.controller

import io.github.wntopia.gikipedia.server.domain.user.service.QueryMyContributionStatsService
import io.github.wntopia.gikipedia.server.domain.user.service.QueryMyContributionsService
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

/** 범위를 벗어난 `limit`이 500이 아니라 400으로 거절되는지(클래스 레벨 @Validated 없이 기본 메서드 검증) 확인한다. */
class UserControllerValidationTest {
    private val contributionsService = mock<QueryMyContributionsService>()
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        val validator = LocalValidatorFactoryBean().apply { afterPropertiesSet() }
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(UserController(mock<QueryMyContributionStatsService>(), contributionsService))
                .setValidator(validator)
                .build()
        whenever(contributionsService.execute(any(), any())).thenReturn(emptyList())
    }

    @Test
    @DisplayName("limit이 범위를 벗어나면 400")
    fun rejectsOutOfRangeLimit() {
        mockMvc.perform(get("/api/v1/users/me/contributions").param("limit", "0")).andExpect(status().isBadRequest)
        mockMvc.perform(get("/api/v1/users/me/contributions").param("limit", "101")).andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("범위 안의 limit은 통과한다")
    fun acceptsValidLimit() {
        mockMvc.perform(get("/api/v1/users/me/contributions").param("limit", "20")).andExpect(status().isOk)
    }
}
