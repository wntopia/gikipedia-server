package io.github.wntopia.gikipedia.server.domain.favorite.entity

import io.github.wntopia.gikipedia.server.global.entity.BaseJpaEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/** 사용자가 즐겨찾기한 문서. 같은 문서는 사용자당 한 번만 들어간다. */
@Entity
@Table(
    name = "article_favorites",
    uniqueConstraints = [UniqueConstraint(name = "uk_favorites_user_article", columnNames = ["user_id", "article_id"])],
    indexes = [Index(name = "idx_favorites_user_created", columnList = "user_id, created_at")],
)
class ArticleFavoriteJpaEntity(
    /** 즐겨찾기한 사용자의 DataGSM 계정 id. */
    @field:Column(name = "user_id", nullable = false, updatable = false) val userId: Long,
    @field:Column(name = "article_id", nullable = false, updatable = false) val articleId: Long,
) : BaseJpaEntity()
