package io.github.wntopia.gikipedia.server.domain.history.service

import io.github.wntopia.gikipedia.server.domain.history.dto.response.ArticleRevisionResDto
import io.github.wntopia.gikipedia.server.domain.history.dto.response.ArticleRevisionSummaryResDto
import io.github.wntopia.gikipedia.server.global.dto.PageResDto

interface ReconstructArticleService {
    /** 특정 리비전 시점의 문서를 스냅샷 + diff 순차 적용으로 재구성한다. */
    fun reconstruct(
        articleId: Long,
        revision: Int,
    ): ArticleRevisionResDto

    /** 문서의 리비전 목록(최신 순)에서 [page]번째 페이지(0부터)를 반환한다. */
    fun listRevisions(
        articleId: Long,
        page: Int,
        size: Int,
    ): PageResDto<ArticleRevisionSummaryResDto>
}
