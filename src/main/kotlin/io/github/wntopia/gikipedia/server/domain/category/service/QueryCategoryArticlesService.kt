package io.github.wntopia.gikipedia.server.domain.category.service

import io.github.wntopia.gikipedia.server.domain.category.dto.response.CategoryArticleResDto
import io.github.wntopia.gikipedia.server.global.dto.PageResDto

interface QueryCategoryArticlesService {
    fun execute(
        categoryId: Long,
        page: Int,
        size: Int,
    ): PageResDto<CategoryArticleResDto>
}
