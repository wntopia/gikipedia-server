package io.github.wntopia.gikipedia.server.domain.category.repository

import io.github.wntopia.gikipedia.server.domain.category.entity.CategoryJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface CategoryRepository : JpaRepository<CategoryJpaEntity, Long> {
    fun existsByName(name: String): Boolean

    /** 분류 목록과 분류별 문서 수. 문서가 없는 분류도 포함한다. */
    @Query(
        "SELECT c.id AS id, c.name AS name, COUNT(a) AS articleCount " +
            "FROM CategoryJpaEntity c LEFT JOIN ArticleJpaEntity a ON a.category = c " +
            "GROUP BY c.id, c.name ORDER BY c.name",
    )
    fun findAllWithArticleCount(): List<CategoryCountView>
}

interface CategoryCountView {
    val id: Long
    val name: String
    val articleCount: Long
}
