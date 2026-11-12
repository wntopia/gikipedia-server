package io.github.wntopia.gikipedia.server.domain.category.service.impl

import io.github.wntopia.gikipedia.server.domain.category.dto.response.CategoryListItemResDto
import io.github.wntopia.gikipedia.server.domain.category.repository.CategoryRepository
import io.github.wntopia.gikipedia.server.domain.category.service.QueryCategoriesService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class QueryCategoriesServiceImpl(
    private val categoryRepository: CategoryRepository,
) : QueryCategoriesService {
    @Transactional(readOnly = true)
    override fun execute(): List<CategoryListItemResDto> =
        categoryRepository.findAllWithArticleCount().map(CategoryListItemResDto::from)
}
