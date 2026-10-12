package io.github.wntopia.gikipedia.server.domain.history.dto

import java.time.Instant

/**
 * 압축 세그먼트 내부에 보존되는 개별(interior) 리비전 메타데이터.
 *
 * [io.github.wntopia.gikipedia.server.domain.history.service.impl.ReconstructArticleServiceImpl]의
 * reconstruct/listRevisions 양쪽 모두 diff 본문뿐 아니라 editor/createdAt도 필요로 하므로, 원본
 * [io.github.wntopia.gikipedia.server.domain.history.entity.ArticleHistoryJpaEntity] row가 가진 정보를
 * 전부 보존한다.
 */
data class ArticleHistorySegmentEntry(
    val revision: Int,
    val editor: String,
    val diff: String,
    val createdAt: Instant,
    // 아래 세 필드는 이 기능 이전에 압축된 세그먼트에는 없으므로 기본값으로 읽는다.
    val summary: String? = null,
    val minorEdit: Boolean = false,
    val charDelta: Int? = null,
)
