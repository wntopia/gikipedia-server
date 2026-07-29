package io.github.wntopia.gikipedia.server.domain.article.repository

import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ArticleRepository : JpaRepository<ArticleJpaEntity, Long> {
    /** 일일 정합성 검사 배치가 훑을 전체 article id 목록. */
    @Query("SELECT a.id FROM ArticleJpaEntity a")
    fun findAllIds(): List<Long>
}
