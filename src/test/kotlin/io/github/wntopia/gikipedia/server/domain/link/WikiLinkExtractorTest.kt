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
    @DisplayName("별칭(|)이 들어간 구문은 링크로 보지 않고, #은 제목의 일부로 본다 — 검색 표시용 변환과 같은 규칙")
    fun aliasIsNotALinkAndHashIsPartOfTitle() {
        assertThat(WikiLinkExtractor.extract("[[기숙사|우리 집]] [[동아리#소개]]")).containsExactly("동아리#소개")
    }

    @Test
    @DisplayName("중첩된 구문은 링크로 보지 않고, 그 뒤의 링크는 계속 찾는다")
    fun nestedIsNotALink() {
        assertThat(WikiLinkExtractor.extract("[[바깥 [[안쪽]] 문서]] 그리고 [[다음]]")).containsExactly("다음")
    }

    @Test
    @DisplayName("닫히지 않은 구문 뒤의 같은 줄은 링크로 보지 않지만, 다음 줄은 다시 찾는다")
    fun unclosedStopsOnlyThatLine() {
        assertThat(WikiLinkExtractor.extract("[[열림만 [[다른]]\n[[다음 줄]]")).containsExactly("다음 줄")
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
