package io.github.wntopia.gikipedia.server.domain.article.service

/** PCRE2와 JVM의 Unicode case folding 차이를 피하고 ASCII 영문만 대소문자를 무시한다. */
object ArticleSearchPattern {
    fun literal(query: String): String =
        buildString {
            query.forEach { character ->
                when (character) {
                    in 'a'..'z', in 'A'..'Z' -> {
                        append('[')
                        append(character.lowercaseChar())
                        append(character.uppercaseChar())
                        append(']')
                    }
                    in "\\.^$|?*+()[]{}" -> {
                        append('\\')
                        append(character)
                    }
                    else -> append(character)
                }
            }
        }
}
