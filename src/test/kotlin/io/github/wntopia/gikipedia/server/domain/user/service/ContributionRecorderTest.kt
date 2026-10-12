package io.github.wntopia.gikipedia.server.domain.user.service

import io.github.wntopia.gikipedia.server.domain.user.entity.ContributionJpaEntity
import io.github.wntopia.gikipedia.server.domain.user.entity.ContributionType
import io.github.wntopia.gikipedia.server.domain.user.repository.ContributionRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class ContributionRecorderTest {
    private val contributionRepository = mock<ContributionRepository>()
    private val recorder = ContributionRecorder(contributionRepository)

    @Test
    @DisplayName("수정 기여는 사용자·문서·리비전과 함께 EDIT로 기록한다")
    fun recordsEdit() {
        recorder.recordEdit(10L, 1L, 3)

        val captor = argumentCaptor<ContributionJpaEntity>()
        verify(contributionRepository).save(captor.capture())
        assertThat(captor.firstValue.type).isEqualTo(ContributionType.EDIT)
        assertThat(captor.firstValue.userId).isEqualTo(10L)
        assertThat(captor.firstValue.articleId).isEqualTo(1L)
        assertThat(captor.firstValue.revision).isEqualTo(3)
    }

    @Test
    @DisplayName("생성 기여는 리비전 없이 CREATE로 기록한다")
    fun recordsCreate() {
        recorder.recordCreate(10L, 1L)

        val captor = argumentCaptor<ContributionJpaEntity>()
        verify(contributionRepository).save(captor.capture())
        assertThat(captor.firstValue.type).isEqualTo(ContributionType.CREATE)
        assertThat(captor.firstValue.revision).isNull()
    }

    @Test
    @DisplayName("사용자를 알 수 없으면 아무것도 기록하지 않는다")
    fun ignoresUnknownUser() {
        recorder.recordCreate(null, 1L)
        recorder.recordEdit(null, 1L, 2)

        verify(contributionRepository, never()).save(any<ContributionJpaEntity>())
    }
}
