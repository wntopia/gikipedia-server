package io.github.wntopia.gikipedia.server.domain.history.entity

import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleJpaEntity
import io.github.wntopia.gikipedia.server.global.entity.BaseJpaEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.Lob
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * 두 스냅샷 사이(fromRevision, toRevision)의 interior 리비전들을 하나의 압축 블록으로 묶어 저장하는 엔티티.
 *
 * [ArticleHistoryJpaEntity]가 리비전마다 row 하나씩 diff를 쌓는 반면, interior 구간이 다음 스냅샷 생성으로
 * "닫히면" 그 구간의 row들을 여기 하나의 gzip 블록(compressedPayload)으로 재인코딩하고 원본 row는 삭제한다.
 * fromRevision/toRevision 자신의 [ArticleHistoryJpaEntity] row는 압축 대상이 아니라 그대로 남는다.
 */
@Entity
@Table(
    name = "article_history_segments",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_history_segment_article_from",
            columnNames = ["article_id", "from_revision"],
        ),
    ],
)
class ArticleHistorySegmentJpaEntity(
    @field:ManyToOne(fetch = FetchType.LAZY)
    @field:JoinColumn(name = "article_id", nullable = false, updatable = false)
    val article: ArticleJpaEntity,
    /** 압축 구간의 시작 스냅샷 리비전. interior는 fromRevision+1부터 시작한다. */
    @field:Column(name = "from_revision", nullable = false, updatable = false) val fromRevision: Int,
    /** 압축 구간의 끝 스냅샷 리비전. interior는 toRevision-1까지다. */
    @field:Column(name = "to_revision", nullable = false, updatable = false) val toRevision: Int,
    /** interior 리비전들(revision/editor/diff/createdAt)을 JSON 직렬화 후 gzip 압축한 바이너리. */
    @field:Lob
    @field:Column(name = "compressed_payload", nullable = false, updatable = false, columnDefinition = "LONGBLOB")
    val compressedPayload: ByteArray,
) : BaseJpaEntity()
