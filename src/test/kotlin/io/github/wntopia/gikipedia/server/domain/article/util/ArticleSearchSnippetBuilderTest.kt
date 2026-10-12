package io.github.wntopia.gikipedia.server.domain.article.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class ArticleSearchSnippetBuilderTest {
    @Nested
    @DisplayName("Describe: build")
    inner class DescribeSubject {
        @Nested
        @DisplayName("Context: 특수문자와 Unicode가 검색어이면")
        inner class ContextLiteralQuery {
            @TestFactory
            @DisplayName("It: 특수문자는 리터럴이고 영문 대소문자만 무시한다")
            fun `특수문자는 리터럴이고 영문 대소문자만 무시한다`() =
                listOf(".*", "[", "]", "\\E", "\$title", "(x)", "급식 안내", "🍱", "'\"", "GSM").map { query ->
                    dynamicTest(query) {
                        // When
                        val parts = ArticleSearchSnippetBuilder.build("앞 $query 뒤", query)

                        // Then
                        assertThat(parts.filter { it.highlight }.map { it.text }).containsExactly(query)
                        assertThat(parts.joinToString("") { it.text }).isEqualTo("앞 $query 뒤")
                    }
                }
        }

        @Nested
        @DisplayName("Context: 영문 대소문자가 섞인 일치가 반복되면")
        inner class ContextRepeatedAsciiMatches {
            @Test
            @DisplayName("It: 영문 반복 일치를 원래 표기로 강조한다")
            fun `영문 반복 일치를 원래 표기로 강조한다`() {
                // When
                val parts = ArticleSearchSnippetBuilder.build("GSM과 gsm 그리고 GsM", "gsm")

                // Then
                assertThat(parts.filter { it.highlight }.map { it.text }).containsExactly("GSM", "gsm", "GsM")
            }
        }

        @Nested
        @DisplayName("Context: ASCII 영문과 닮은 Unicode 문자가 있으면")
        inner class ContextUnicodeCase {
            @Test
            @DisplayName("It: Unicode 문자를 ASCII 영문으로 접지 않는다")
            fun `Unicode 문자를 ASCII 영문으로 접지 않는다`() {
                // When
                val parts = ArticleSearchSnippetBuilder.build("İ ı ſ K I i S s K k", "i")

                // Then
                assertThat(parts.filter { it.highlight }.map { it.text }).containsExactly("I", "i")
            }
        }

        @Nested
        @DisplayName("Context: 첫 일치의 위치가 달라지면")
        inner class ContextMatchPosition {
            @TestFactory
            @DisplayName("It: 처음 중간 끝의 검색어를 온전히 포함한다")
            fun `처음 중간 끝의 검색어를 온전히 포함한다`() =
                listOf(0, 80, 300).map { prefixLength ->
                    dynamicTest("일치 앞 길이 $prefixLength") {
                        val query = "급".repeat(100)
                        val text = "가".repeat(prefixLength) + query + "나".repeat(300 - prefixLength)
                        // When
                        val parts = ArticleSearchSnippetBuilder.build(text, query)

                        // Then
                        assertThat(parts.filter { it.highlight }.map { it.text }).containsExactly(query)
                        val excerpt = parts.joinToString("") { it.text }
                        assertThat(excerpt.codePointCount(0, excerpt.length)).isLessThanOrEqualTo(162)
                        assertThat(excerpt.startsWith("…")).isEqualTo(prefixLength > 40)
                    }
                }
        }

        @Nested
        @DisplayName("Context: 본문에서 검색어를 찾지 못하면")
        inner class ContextNoBodyMatch {
            @Test
            @DisplayName("It: 본문 일치가 없으면 앞부분을 반환하고 빈 본문이면 빈 배열이다")
            fun `본문 일치가 없으면 앞부분을 반환하고 빈 본문이면 빈 배열이다`() {
                // When
                val parts = ArticleSearchSnippetBuilder.build("가".repeat(200), "제목")

                // Then
                assertThat(parts.joinToString("") { it.text }).isEqualTo("가".repeat(160) + "…")
                assertThat(parts.none { it.highlight }).isTrue()
                assertThat(ArticleSearchSnippetBuilder.build("", "제목")).isEmpty()
            }
        }

        @Nested
        @DisplayName("Context: 발췌 끝에 grapheme이 걸치면")
        inner class ContextEndBoundary {
            @Test
            @DisplayName("It: 발췌 끝의 ZWJ 이모지와 결합문자를 자르지 않는다")
            fun `발췌 끝의 ZWJ 이모지와 결합문자를 자르지 않는다`() {
                for (cluster in listOf("👨‍👩‍👧‍👦", "e\u0301", "🍱")) {
                    // When
                    val parts = ArticleSearchSnippetBuilder.build("가".repeat(159) + cluster + "끝", "제목")

                    // Then
                    assertThat(parts.joinToString("") { it.text }).isEqualTo("가".repeat(159) + cluster + "…")
                }
            }
        }

        @Nested
        @DisplayName("Context: 발췌 시작에 grapheme이 걸치면")
        inner class ContextStartBoundary {
            @Test
            @DisplayName("It: 발췌 시작의 grapheme을 자르지 않는다")
            fun `발췌 시작의 grapheme을 자르지 않는다`() {
                val family = "👨‍👩‍👧‍👦"
                val text = "가".repeat(100) + family + "나".repeat(36) + "급식" + "다".repeat(200)
                // When
                val parts = ArticleSearchSnippetBuilder.build(text, "급식")

                // Then
                assertThat(parts.joinToString("") { it.text }).startsWith("…$family")
                assertThat(parts.filter { it.highlight }.map { it.text }).containsExactly("급식")
            }
        }

        @Nested
        @DisplayName("Context: 일치 구간이 겹치면")
        inner class ContextOverlappingMatches {
            @Test
            @DisplayName("It: 겹치는 일치는 왼쪽부터 겹치지 않게 강조한다")
            fun `겹치는 일치는 왼쪽부터 겹치지 않게 강조한다`() {
                // When
                val parts = ArticleSearchSnippetBuilder.build("aaaaa", "aa")

                // Then
                assertThat(parts.filter { it.highlight }.joinToString("") { it.text }).isEqualTo("aaaa")
                assertThat(parts.last().text).isEqualTo("a")
                assertThat(parts.all { it.text.isNotEmpty() }).isTrue()
            }
        }
    }
}
