package io.github.wntopia.gikipedia.server.domain.link

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class WikiLinkExtractorTest {
    @Test
    @DisplayName("본문의 [[문서명]]을 등장 순서대로 뽑는다")
    fun extractsLinks() {
        assertThat(WikiLinkExtractor.extract("== 개요 ==\n[[기숙사]]와 [[동아리 안내]]를 보세요.")).containsExactly("기숙사", "동아리 안내")
    }

    @Test
    @DisplayName("같은 제목은 공백·대소문자가 달라도 한 번만 반환한다")
    fun deduplicates() {
        assertThat(WikiLinkExtractor.extract("[[Home]] [[ home ]] [[HOME]]")).containsExactly("Home")
    }

    @Test
    @DisplayName("표시 문구(|)와 문단(#)은 떼고 문서명만 본다")
    fun stripsAliasAndAnchor() {
        assertThat(WikiLinkExtractor.extract("[[기숙사|우리 집]] [[동아리#소개]]")).containsExactly("기숙사", "동아리")
    }

    @Test
    @DisplayName("빈 링크, 줄바꿈이 낀 링크, 너무 긴 제목은 무시한다")
    fun ignoresInvalid() {
        val tooLong = "가".repeat(256)
        assertThat(WikiLinkExtractor.extract("[[]] [[   ]] [[줄\n바꿈]] [[$tooLong]]")).isEmpty()
    }

    @Test
    @DisplayName("링크가 없는 본문은 빈 목록이다")
    fun noLinks() {
        assertThat(WikiLinkExtractor.extract("그냥 [단일 대괄호] 본문")).isEmpty()
    }
}
