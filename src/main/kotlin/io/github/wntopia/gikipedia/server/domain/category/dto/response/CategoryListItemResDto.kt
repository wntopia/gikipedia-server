package io.github.wntopia.gikipedia.server.domain.category.dto.response

import io.github.wntopia.gikipedia.server.domain.category.repository.CategoryCountView
import io.swagger.v3.oas.annotations.media.Schema

data class CategoryListItemResDto(
    @field:Schema(description = "분류 ID", example = "1")
    val id: Long,
    @field:Schema(description = "분류 이름", example = "학교")
    val name: String,
    @field:Schema(description = "이 분류에 속한 문서 수", example = "12")
    val articleCount: Long,
) {
    companion object {
        fun from(view: CategoryCountView): CategoryListItemResDto =
            CategoryListItemResDto(view.id, view.name, view.articleCount)
    }
}
