package io.github.wntopia.gikipedia.server.domain.category.service.impl

import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.domain.category.dto.response.CategoryArticleResDto
import io.github.wntopia.gikipedia.server.domain.category.repository.CategoryRepository
import io.github.wntopia.gikipedia.server.domain.category.service.QueryCategoryArticlesService
import io.github.wntopia.gikipedia.server.global.dto.PageResDto
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import team.themoment.sdk.exception.ExpectedException

@Service
class QueryCategoryArticlesServiceImpl(
    private val categoryRepository: CategoryRepository,
    private val articleRepository: ArticleRepository,
) : QueryCategoryArticlesService {
    @Transactional(readOnly = true)
    override fun execute(
        categoryId: Long,
        page: Int,
        size: Int,
    ): PageResDto<CategoryArticleResDto> {
        if (!categoryRepository.existsById(categoryId)) {
            throw ExpectedException("존재하지 않는 분류입니다.", HttpStatus.NOT_FOUND)
        }
        val result = articleRepository.findByCategory(categoryId, PageRequest.of(page, size))
        return PageResDto(
            result.content.map(CategoryArticleResDto::from),
            page,
            size,
            result.totalElements,
            result.hasNext(),
        )
    }
}
