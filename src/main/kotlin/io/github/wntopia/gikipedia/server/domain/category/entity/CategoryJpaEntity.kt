package io.github.wntopia.gikipedia.server.domain.category.entity

import io.github.wntopia.gikipedia.server.global.entity.BaseJpaEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * 문서 분류(학교 / 동아리 / 행사 / 인물 등). 문서 간 계층 구조(`ArticleJpaEntity.parent`)와는 별개의 평면 목록이다.
 * 분류 이름은 만든 뒤 바꾸지 않는 것을 전제로, Mongo 읽기 뷰와 캐시에 이름을 그대로 복사해 둔다.
 */
@Entity
@Table(name = "categories", uniqueConstraints = [UniqueConstraint(name = "uk_categories_name", columnNames = ["name"])])
class CategoryJpaEntity(
    @field:Column(name = "name", nullable = false, updatable = false, length = 50) val name: String,
) : BaseJpaEntity()
