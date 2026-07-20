package io.github.wntopia.gikipedia.server.domain.history.repository

import io.github.wntopia.gikipedia.server.domain.history.entity.ArticleSnapshotJpaEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ArticleSnapshotRepository : JpaRepository<ArticleSnapshotJpaEntity, Long> {
    /** 대상 리비전 이하에서 가장 가까운(리비전이 가장 큰) 스냅샷. 재구성의 시작점이 된다. */
    fun findTopByArticleIdAndRevisionLessThanEqualOrderByRevisionDesc(
        articleId: Long,
        revision: Int,
    ): ArticleSnapshotJpaEntity?
}
