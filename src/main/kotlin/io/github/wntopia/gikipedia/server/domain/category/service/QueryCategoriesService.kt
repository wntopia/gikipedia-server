package io.github.wntopia.gikipedia.server.domain.category.service

import io.github.wntopia.gikipedia.server.domain.category.dto.response.CategoryListItemResDto

interface QueryCategoriesService {
    fun execute(): List<CategoryListItemResDto>
}
