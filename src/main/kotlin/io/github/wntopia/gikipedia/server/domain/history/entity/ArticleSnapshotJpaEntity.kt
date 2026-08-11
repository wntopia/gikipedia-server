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
 * 특정 리비전 시점의 문서 전체 내용을 저장하는 스냅샷 엔티티.
 *
 * [ArticleHistoryJpaEntity]가 리비전마다 diff 조각만 쌓는 반면, 이 테이블은 일정 주기(article.snapshot.interval, 기본 10)마다 전체 내용을 통째로 저장한다. 특정 리비전 문서를
 * 재구성할 때는 대상 리비전 이하에서 가장 가까운 스냅샷을 기준으로 삼고, 그 이후의 diff만 순차 적용한다. 덕분에 재구성 비용이 리비전 수에 비례하지 않고 스냅샷 간격(최대 N-1개 diff)으로 상한이
 * 고정된다.
 */
@Entity
@Table(
    name = "article_snapshots",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_snapshot_article_revision",
            columnNames = ["article_id", "revision"],
        ),
    ],
)
class ArticleSnapshotJpaEntity(
    @field:ManyToOne(fetch = FetchType.LAZY)
    @field:JoinColumn(name = "article_id", nullable = false, updatable = false)
    val article: ArticleJpaEntity,
    /** 이 스냅샷이 나타내는 리비전 번호. */
    @field:Column(name = "revision", nullable = false, updatable = false) val revision: Int,
    /** 해당 리비전 시점의 문서 전체 내용. */
    @field:Column(name = "content", nullable = false, updatable = false, columnDefinition = "TEXT")
    val content: String,
) : BaseJpaEntity() {
    /**
     * 이 스냅샷(revision)과 다음 스냅샷 사이의 interior 리비전들을 압축해서 담아두는 페이로드.
     *
     * [io.github.wntopia.gikipedia.server.domain.history.service.ArticleHistoryCompactor]가 다음
     * 스냅샷이 생긴 뒤 배치로 채워 넣는다(쓰기 시점에는 항상 null). null이면 아직 압축되지 않아 interior
     * row가 [ArticleHistoryJpaEntity]에 그대로 남아있다는 뜻이다.
     */
    @field:Lob
    @field:Column(name = "compressed_interior_payload", columnDefinition = "LONGBLOB")
    var compressedInteriorPayload: ByteArray? = null
        protected set

    fun attachCompressedInteriorPayload(payload: ByteArray) {
        this.compressedInteriorPayload = payload
    }
}
