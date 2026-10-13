package io.github.wntopia.gikipedia.server.domain.link

import io.github.wntopia.gikipedia.server.global.text.TitleNormalizer

/**
 * 본문에서 위키 링크(`[[문서명]]`)의 대상 제목을 뽑는다.
 *
 * 문서 검색의 표시용 텍스트 변환(#32의 `ArticleSearchTextConverter`)과 같은 규칙으로 링크를 판정한다 — 같은 본문을 두 기능이
 * 서로 다르게 해석하지 않도록 하기 위함이다.
 * - 한 줄 안에서 `[[`와 짝이 맞는 `]]` 사이를 링크 후보로 본다. 닫히지 않으면 그 줄의 나머지는 링크가 아니다.
 * - 안에 또 `[[`가 있는(중첩) 후보, 비었거나 `[`·`]`·`|`가 들어간 후보는 지원하지 않는 문법이라 링크로 보지 않는다.
 *   (`[[문서명|표시]]` 같은 별칭 문법은 지원하지 않는다. `#`은 제목의 일부로 취급한다.)
 *
 * 제목은 [TitleNormalizer]로 정규화하고, 비었거나 255자를 넘는 것은 제외한다. 같은 제목이 여러 번 나와도 한 번만 반환한다(대소문자 무시).
 */
object WikiLinkExtractor {
    private const val MAX_TITLE_LENGTH = 255
    private const val OPEN = "[["
    private const val CLOSE = "]]"

    fun extract(content: String): List<String> =
        content
            .lineSequence()
            .flatMap(::linksInLine)
            .map(TitleNormalizer::normalize)
            .filter { it.isNotEmpty() && it.length <= MAX_TITLE_LENGTH }
            .distinctBy(TitleNormalizer::keyOf)
            .toList()

    private fun linksInLine(line: String): List<String> {
        val links = mutableListOf<String>()
        var index = line.indexOf(OPEN)
        while (index >= 0) {
            val (end, nested) = findClosing(line, index + OPEN.length) ?: break
            val label = line.substring(index + OPEN.length, end - CLOSE.length)
            if (!nested && label.isNotBlank() && label.none { it in "[]|" }) links += label
            index = line.indexOf(OPEN, end)
        }
        return links
    }

    /** [from]부터 짝이 맞는 `]]`의 바로 뒤 위치와 중첩 여부. 닫히지 않으면 null. */
    private fun findClosing(
        line: String,
        from: Int,
    ): Pair<Int, Boolean>? {
        var index = from
        var depth = 1
        var nested = false
        while (index < line.length) {
            when {
                line.startsWith(OPEN, index) -> {
                    depth++
                    nested = true
                    index += OPEN.length
                }
                line.startsWith(CLOSE, index) -> {
                    depth--
                    index += CLOSE.length
                    if (depth == 0) return index to nested
                }
                else -> index++
            }
        }
        return null
    }
}
