package io.github.wntopia.gikipedia.server.domain.category.service

import io.github.wntopia.gikipedia.server.domain.category.dto.request.CreateCategoryReqDto
import io.github.wntopia.gikipedia.server.domain.category.dto.response.CategoryResDto
import jakarta.servlet.http.HttpSession

interface CreateCategoryService {
    fun execute(
        reqDto: CreateCategoryReqDto,
        session: HttpSession,
    ): CategoryResDto
}
