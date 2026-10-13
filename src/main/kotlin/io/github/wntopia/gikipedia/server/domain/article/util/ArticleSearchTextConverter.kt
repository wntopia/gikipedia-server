package io.github.wntopia.gikipedia.server.domain.article.util

/** 지원하는 위키 문법만 제거한다. 미완성·중첩·미지원 구문은 텍스트로 남긴다. */
object ArticleSearchTextConverter {
    private val whitespace = Regex("[\\p{javaWhitespace}\\p{Z}]+")

    fun convert(content: String): String =
        content
            .lineSequence()
            .joinToString(" ") { line ->
                stripLinks(stripHeading(line))
            }.replace(whitespace, " ")
            .trim()

    private fun stripHeading(line: String): String {
        val trimmed = line.trim()
        if (trimmed.length < 5 ||
            !trimmed.startsWith("==") ||
            !trimmed.endsWith("==") ||
            trimmed.startsWith("===") ||
            trimmed.endsWith("===")
        ) {
            return line
        }
        return trimmed.substring(2, trimmed.length - 2).trim().takeIf { it.isNotEmpty() } ?: line
    }

    private fun stripLinks(line: String): String =
        buildString {
            var index = 0
            while (index < line.length) {
                if (!line.startsWith("[[", index)) {
                    append(line[index++])
                    continue
                }
                val start = index
                index += 2
                var depth = 1
                var nested = false
                while (index < line.length && depth > 0) {
                    when {
                        line.startsWith("[[", index) -> {
                            depth++
                            nested = true
                            index += 2
                        }
                        line.startsWith("]]", index) -> {
                            depth--
                            index += 2
                        }
                        else -> index++
                    }
                }
                if (depth != 0) {
                    append(line, start, line.length)
                    break
                }
                val label = line.substring(start + 2, index - 2)
                if (!nested && label.isNotBlank() && label.none { it in "[]|" }) {
                    append(label)
                } else {
                    append(line, start, index)
                }
            }
        }
}
