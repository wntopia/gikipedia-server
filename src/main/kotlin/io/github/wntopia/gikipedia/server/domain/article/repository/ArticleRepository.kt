package io.github.wntopia.gikipedia.server.domain.article.repository

import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleJpaEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ArticleRepository : JpaRepository<ArticleJpaEntity, Long>
