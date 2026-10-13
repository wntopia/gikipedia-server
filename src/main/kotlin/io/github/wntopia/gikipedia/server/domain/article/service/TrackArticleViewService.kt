package io.github.wntopia.gikipedia.server.domain.article.service

import jakarta.servlet.http.HttpServletRequest

interface TrackArticleViewService {
    /** 조회를 기록하고 현재 누적 조회수를 반환한다. 집계에 실패해도 예외를 던지지 않는다(null). */
    fun execute(
        articleId: Long,
        request: HttpServletRequest,
    ): Long?
}
