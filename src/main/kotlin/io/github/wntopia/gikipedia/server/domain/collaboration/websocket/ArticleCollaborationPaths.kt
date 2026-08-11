package io.github.wntopia.gikipedia.server.domain.collaboration.websocket

import java.net.URI

/**
 * `/ws/articles/{articleId}/collaboration` 경로에서 articleId를 뽑아낸다.
 *
 * `WebSocketHandlerRegistry`는 named path-variable 추출을 지원하지 않으므로, 별도 의존성 없이 URI를 직접
 * 정규식으로 파싱하는 편이 가장 단순하다. handshake 인터셉터와 핸들러 양쪽에서 공유한다.
 */
object ArticleCollaborationPaths {
    private val ARTICLE_ID_PATTERN = Regex("""/ws/articles/(\d+)/collaboration$""")

    fun articleIdOf(uri: URI?): Long? =
        uri?.path?.let {
            ARTICLE_ID_PATTERN
                .find(it)
                ?.groupValues
                ?.get(1)
                ?.toLongOrNull()
        }
}
