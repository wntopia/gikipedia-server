package io.github.wntopia.gikipedia.server.domain.article.util

import io.github.wntopia.gikipedia.server.domain.article.dto.response.SnippetSegmentResDto
import java.util.regex.Pattern

object ArticleSearchSnippetBuilder {
    private const val EXCERPT_LENGTH = 160
    private const val LEADING_CONTEXT = 40
    private val grapheme = Pattern.compile("\\X")

    fun build(
        text: String,
        query: String,
    ): List<SnippetSegmentResDto> {
        require(query.isNotEmpty())
        if (text.isEmpty()) return emptyList()
        val pattern = Pattern.compile(ArticleSearchPattern.literal(query))
        val firstMatch = pattern.matcher(text)
        val firstIndex = if (firstMatch.find()) firstMatch.start() else 0
        val startPoint = (text.codePointCount(0, firstIndex) - LEADING_CONTEXT).coerceAtLeast(0)
        val endPoint = (startPoint + EXCERPT_LENGTH).coerceAtMost(text.codePointCount(0, text.length))
        val requestedStart = text.offsetByCodePoints(0, startPoint)
        val requestedEnd = text.offsetByCodePoints(0, endPoint)

        // 경계에 걸친 결합문자/ZWJ 이모지는 통째로 포함한다. 이때 160 코드포인트를 넘을 수 있다.
        var start = 0
        var end = text.length
        val boundaries = grapheme.matcher(text)
        while (boundaries.find()) {
            if (boundaries.start() <= requestedStart) start = boundaries.start()
            if (boundaries.end() >= requestedEnd) {
                end = boundaries.end()
                break
            }
        }
        val excerpt = text.substring(start, end)
        val segments = mutableListOf<SnippetSegmentResDto>()

        fun append(
            value: String,
            highlight: Boolean,
        ) {
            if (value.isEmpty()) return
            val previous = segments.lastOrNull()
            if (previous != null && previous.highlight == highlight) {
                segments[segments.lastIndex] = previous.copy(text = previous.text + value)
            } else {
                segments.add(SnippetSegmentResDto(value, highlight))
            }
        }
        if (start > 0) append("…", false)
        val matches = pattern.matcher(excerpt)
        var cursor = 0
        while (matches.find()) {
            append(excerpt.substring(cursor, matches.start()), false)
            append(matches.group(), true)
            cursor = matches.end()
        }
        append(excerpt.substring(cursor), false)
        if (end < text.length) append("…", false)
        return segments
    }
}
