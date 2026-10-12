package io.github.wntopia.gikipedia.server.domain.link.entity

import io.github.wntopia.gikipedia.server.global.entity.BaseJpaEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.Collate

/**
 * "문서 A가 제목 B를 `[[B]]`로 링크한다"는 관계. 대상은 문서 id가 아니라 제목으로 저장한다 — 아직 만들어지지 않은
 * 문서의 제목도 링크될 수 있고, 나중에 그 제목으로 문서가 생기면 자연스럽게 연결되기 때문이다.
 */
@Entity
@Table(
    name = "article_links",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_links_source_target",
            columnNames = ["source_article_id", "target_key"],
        ),
    ],
    indexes = [Index(name = "idx_links_target_key", columnList = "target_key")],
)
class ArticleLinkJpaEntity(
    @field:Column(name = "source_article_id", nullable = false, updatable = false) val sourceArticleId: Long,
    /** 정규화된 대상 제목(원래 표기). */
    @field:Column(name = "target_title", nullable = false, updatable = false, length = 255) val targetTitle: String,
    /**
     * 대소문자 무시 비교용 키 — [io.github.wntopia.gikipedia.server.global.text.TitleNormalizer.keyOf].
     * 키 자체가 이미 비교 기준이므로 DB는 바이너리로 비교한다. 기본 collation은 악센트까지 무시해 앱에서 다른 키가
     * 유니크 제약에서 충돌한다.
     */
    @field:Column(name = "target_key", nullable = false, updatable = false, length = 255)
    @field:Collate("utf8mb4_bin")
    val targetKey: String,
) : BaseJpaEntity()
