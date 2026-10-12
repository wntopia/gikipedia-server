package io.github.wntopia.gikipedia.server.domain.article.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class ArticleSearchSnippetBuilderTest {
    @TestFactory
    fun `특수문자는 리터럴이고 영문 대소문자만 무시한다`() =
        listOf(".*", "[", "]", "\\E", "\$title", "(x)", "급식 안내", "🍱", "'\"", "GSM").map { query ->
            dynamicTest(query) {
                val parts = ArticleSearchSnippetBuilder.build("앞 $query 뒤", query)
                assertThat(parts.filter { it.highlight }.map { it.text }).containsExactly(query)
                assertThat(parts.joinToString("") { it.text }).isEqualTo("앞 $query 뒤")
            }
        }

    @Test
    fun `영문 반복 일치를 원래 표기로 강조한다`() {
        val parts = ArticleSearchSnippetBuilder.build("GSM과 gsm 그리고 GsM", "gsm")
        assertThat(parts.filter { it.highlight }.map { it.text }).containsExactly("GSM", "gsm", "GsM")
    }

    @Test
    fun `Unicode 문자를 ASCII 영문으로 접지 않는다`() {
        val parts = ArticleSearchSnippetBuilder.build("İ ı ſ K I i S s K k", "i")
        assertThat(parts.filter { it.highlight }.map { it.text }).containsExactly("I", "i")
    }

    @TestFactory
    fun `처음 중간 끝의 검색어를 온전히 포함한다`() =
        listOf(0, 80, 300).map { prefixLength ->
            dynamicTest("일치 앞 길이 $prefixLength") {
                val query = "급".repeat(100)
                val text = "가".repeat(prefixLength) + query + "나".repeat(300 - prefixLength)
                val parts = ArticleSearchSnippetBuilder.build(text, query)
                assertThat(parts.filter { it.highlight }.map { it.text }).containsExactly(query)
                val excerpt = parts.joinToString("") { it.text }
                assertThat(excerpt.codePointCount(0, excerpt.length)).isLessThanOrEqualTo(162)
                assertThat(excerpt.startsWith("…")).isEqualTo(prefixLength > 40)
            }
        }

    @Test
    fun `본문 일치가 없으면 앞부분을 반환하고 빈 본문이면 빈 배열이다`() {
        val parts = ArticleSearchSnippetBuilder.build("가".repeat(200), "제목")
        assertThat(parts.joinToString("") { it.text }).isEqualTo("가".repeat(160) + "…")
        assertThat(parts.none { it.highlight }).isTrue()
        assertThat(ArticleSearchSnippetBuilder.build("", "제목")).isEmpty()
    }

    @Test
    fun `발췌 끝의 ZWJ 이모지와 결합문자를 자르지 않는다`() {
        for (cluster in listOf("👨‍👩‍👧‍👦", "e\u0301", "🍱")) {
            val parts = ArticleSearchSnippetBuilder.build("가".repeat(159) + cluster + "끝", "제목")
            assertThat(parts.joinToString("") { it.text }).isEqualTo("가".repeat(159) + cluster + "…")
        }
    }

    @Test
    fun `발췌 시작의 grapheme을 자르지 않는다`() {
        val family = "👨‍👩‍👧‍👦"
        val text = "가".repeat(100) + family + "나".repeat(36) + "급식" + "다".repeat(200)
        val parts = ArticleSearchSnippetBuilder.build(text, "급식")
        assertThat(parts.joinToString("") { it.text }).startsWith("…$family")
        assertThat(parts.filter { it.highlight }.map { it.text }).containsExactly("급식")
    }

    @Test
    fun `겹치는 일치는 왼쪽부터 겹치지 않게 강조한다`() {
        val parts = ArticleSearchSnippetBuilder.build("aaaaa", "aa")
        assertThat(parts.filter { it.highlight }.joinToString("") { it.text }).isEqualTo("aaaa")
        assertThat(parts.last().text).isEqualTo("a")
        assertThat(parts.all { it.text.isNotEmpty() }).isTrue()
    }
}
