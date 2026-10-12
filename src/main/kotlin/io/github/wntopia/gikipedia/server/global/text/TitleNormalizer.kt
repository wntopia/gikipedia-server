package io.github.wntopia.gikipedia.server.global.text

import java.text.Normalizer
import java.util.Locale

/**
 * 문서 제목의 정규화 규칙. 생성·조회·링크 해석이 모두 같은 규칙을 거쳐야 같은 제목이 같은 문서로 해석된다.
 *
 * 앞뒤 공백 제거 → NFC 정규화(조합형/분리형 한글 통일) → 연속 공백을 한 칸으로 축소.
 */
object TitleNormalizer {
    /** 저장할 수 있는 제목의 최대 길이(`articles.title` 컬럼 길이). 이보다 긴 제목의 문서는 존재할 수 없다. */
    const val MAX_LENGTH = 255

    private val WHITESPACES = Regex("\\s+")

    fun normalize(title: String): String =
        Normalizer.normalize(title, Normalizer.Form.NFC).trim().replace(WHITESPACES, " ")

    /**
     * 제목끼리 같은지 비교할 때 쓰는 키. MySQL 기본 collation이 대소문자를 구분하지 않으므로
     * DB의 유니크 판정과 같은 기준이 되도록 소문자로 맞춘다.
     */
    fun keyOf(title: String): String = normalize(title).lowercase(Locale.ROOT)
}
