package io.github.wntopia.gikipedia.server.domain.link

import io.github.wntopia.gikipedia.server.global.text.TitleNormalizer

/**
 * 본문에서 위키 링크(`[[문서명]]`)의 대상 제목을 뽑는다.
 *
 * `[[문서명|표시 문구]]`는 `|` 앞, `[[문서명#문단]]`은 `#` 앞을 대상 제목으로 본다. 제목은 [TitleNormalizer]로
 * 정규화하고, 비었거나 255자를 넘는 것은 제외한다. 같은 제목이 여러 번 나와도 한 번만 반환한다(대소문자 무시).
 */
object WikiLinkExtractor {
    private val LINK = Regex("\\[\\[([^\\[\\]\\n]+)]]")
    private const val MAX_TITLE_LENGTH = 255

    fun extract(content: String): List<String> =
        LINK
            .findAll(content)
            .map { it.groupValues[1].substringBefore('|').substringBefore('#') }
            .map(TitleNormalizer::normalize)
            .filter { it.isNotEmpty() && it.length <= MAX_TITLE_LENGTH }
            .distinctBy(TitleNormalizer::keyOf)
            .toList()
}
