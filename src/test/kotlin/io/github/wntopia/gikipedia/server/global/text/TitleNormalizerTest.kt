package io.github.wntopia.gikipedia.server.global.text

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class TitleNormalizerTest {
    @Test
    @DisplayName("앞뒤 공백을 제거하고 연속 공백을 한 칸으로 줄인다")
    fun trimsAndCollapsesWhitespace() {
        assertThat(TitleNormalizer.normalize("  기숙사   생활\t안내 ")).isEqualTo("기숙사 생활 안내")
    }

    @Test
    @DisplayName("분리형 한글을 조합형(NFC)으로 통일한다")
    fun normalizesToNfc() {
        val decomposed = "가" // ㄱ + ㅏ
        assertThat(TitleNormalizer.normalize(decomposed)).isEqualTo("가")
    }

    @Test
    @DisplayName("비교 키는 대소문자를 구분하지 않는다")
    fun keyIgnoresCase() {
        assertThat(TitleNormalizer.keyOf(" Gikipedia  Home ")).isEqualTo(TitleNormalizer.keyOf("gikipedia home"))
    }
}
