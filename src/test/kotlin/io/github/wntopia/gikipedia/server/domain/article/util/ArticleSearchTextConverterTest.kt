package io.github.wntopia.gikipedia.server.domain.article.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class ArticleSearchTextConverterTest {
    @Nested
    @DisplayName("Describe: convert")
    inner class DescribeSubject {
        @Nested
        @DisplayName("Context: 지원 문법과 미지원 문법이 입력되면")
        inner class ContextWikiSyntax {
            @TestFactory
            @DisplayName("It: 지원 문법만 변환하고 블록 경계는 공백으로 유지한다")
            fun `지원 문법만 변환하고 블록 경계는 공백으로 유지한다`() =
                listOf(
                    "" to "",
                    " \t\n\u00a0 " to "",
                    "== 개요 ==\n학교 [[급식]] 안내" to "개요 학교 급식 안내",
                    "== [[급식]] 안내 ==" to "급식 안내",
                    "[[급식]][[안내]]" to "급식안내",
                    "[[없는 문서]] 안내" to "없는 문서 안내",
                    "\t급식\t\t안내\r\n이용\u00a0방법" to "급식 안내 이용 방법",
                    "닫히지 않은 [[급식" to "닫히지 않은 [[급식",
                    "[[급식\n안내]]" to "[[급식 안내]]",
                    "[[대상|별칭]]" to "[[대상|별칭]]",
                    "[[A[[B]]C]]" to "[[A[[B]]C]]",
                    "[[A[B]]]" to "[[A[B]]]",
                    "[[ ]] [[]]" to "[[ ]] [[]]",
                    "=== 제목 ===" to "=== 제목 ===",
                    "== 제목 == 뒤" to "== 제목 == 뒤",
                    "== ==" to "== ==",
                    "== 제목" to "== 제목",
                    "<script>alert(1)</script> **강조**" to "<script>alert(1)</script> **강조**",
                    "🍱 [[급식]]" to "🍱 급식",
                ).map { (input, expected) ->
                    dynamicTest(input.ifEmpty { "빈 본문" }) {
                        // When
                        val result = ArticleSearchTextConverter.convert(input)

                        // Then
                        assertThat(result).isEqualTo(expected)
                    }
                }
        }

        @Nested
        @DisplayName("Context: 닫히지 않은 링크 표식이 반복되면")
        inner class ContextLongUnclosedLinks {
            @Test
            @DisplayName("It: 닫히지 않은 표식이 많은 긴 입력도 보존한다")
            fun `닫히지 않은 표식이 많은 긴 입력도 보존한다`() {
                // Given
                val input = "[[".repeat(30_000)

                // When
                val result = ArticleSearchTextConverter.convert(input)

                // Then
                assertThat(result).isEqualTo(input)
            }
        }
    }
}
