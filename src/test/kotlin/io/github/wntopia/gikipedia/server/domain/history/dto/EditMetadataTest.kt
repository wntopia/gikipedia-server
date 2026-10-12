package io.github.wntopia.gikipedia.server.domain.history.dto

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class EditMetadataTest {
    @Test
    @DisplayName("요약의 앞뒤 공백을 제거하고, 비어 있으면 null로 만든다")
    fun trimsAndNullsBlankSummary() {
        assertThat(EditMetadata("  요약  ").normalized().summary).isEqualTo("요약")
        assertThat(EditMetadata("   ").normalized().summary).isNull()
        assertThat(EditMetadata(null).normalized().summary).isNull()
    }

    @Test
    @DisplayName("500자를 넘는 요약은 500자로 자른다")
    fun truncatesLongSummary() {
        assertThat(EditMetadata("가".repeat(600)).normalized().summary).hasSize(EditMetadata.MAX_SUMMARY_LENGTH)
    }
}
