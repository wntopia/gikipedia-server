package io.github.wntopia.gikipedia.server.domain.article.entity

import io.github.wntopia.gikipedia.server.domain.category.entity.CategoryJpaEntity
import io.github.wntopia.gikipedia.server.global.entity.BaseJpaEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.Collate

@Entity
@Table(
    name = "articles",
    uniqueConstraints = [UniqueConstraint(name = "uk_articles_title", columnNames = ["title"])],
)
class ArticleJpaEntity(
    title: String,
    content: String,
    imageUrl: String? = null,
    @field:ManyToOne(fetch = FetchType.LAZY)
    @field:JoinColumn(name = "parent_id")
    val parent: ArticleJpaEntity? = null,
    /** 문서를 만든 사용자의 DataGSM 계정 id. 비로그인 생성이거나 이전 데이터면 null. */
    @field:Column(name = "created_by_user_id", updatable = false) val createdByUserId: Long? = null,
) : BaseJpaEntity() {
    /**
     * 악센트는 구분하고 대소문자·전각/반각은 무시하는 collation. 기본값(`utf8mb4_0900_ai_ci`)은 악센트까지
     * 무시해 [io.github.wntopia.gikipedia.server.global.text.TitleNormalizer.keyOf]와 판정이 갈린다.
     */
    @field:Column(name = "title", nullable = false, length = 255)
    @field:Collate("utf8mb4_0900_as_ci")
    var title: String = title
        protected set

    @field:Column(name = "content", nullable = false, columnDefinition = "TEXT")
    var content: String = content
        protected set

    @field:Column(name = "image_url", length = 2048)
    var imageUrl: String? = imageUrl
        protected set

    @field:ManyToOne(fetch = FetchType.LAZY)
    @field:JoinColumn(name = "category_id")
    var category: CategoryJpaEntity? = null
        protected set

    fun changeCategory(category: CategoryJpaEntity?) {
        this.category = category
    }

    /** title은 생성 시점에만 정해지고 이후 수정 불가능하다 — 여기서 받지 않는다. */
    fun update(
        content: String,
        imageUrl: String?,
    ) {
        this.content = content
        this.imageUrl = imageUrl
    }
}
