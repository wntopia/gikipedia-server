package io.github.wntopia.gikipedia.server.domain.category.service.impl

import io.github.wntopia.gikipedia.server.domain.category.dto.request.CreateCategoryReqDto
import io.github.wntopia.gikipedia.server.domain.category.dto.response.CategoryResDto
import io.github.wntopia.gikipedia.server.domain.category.entity.CategoryJpaEntity
import io.github.wntopia.gikipedia.server.domain.category.repository.CategoryRepository
import io.github.wntopia.gikipedia.server.domain.category.service.CreateCategoryService
import io.github.wntopia.gikipedia.server.global.security.session.AuthenticationReader
import io.github.wntopia.gikipedia.server.global.text.TitleNormalizer
import jakarta.servlet.http.HttpSession
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import team.themoment.sdk.exception.ExpectedException

@Service
class CreateCategoryServiceImpl(
    private val authenticationReader: AuthenticationReader,
    private val categoryRepository: CategoryRepository,
) : CreateCategoryService {
    @Transactional
    override fun execute(
        reqDto: CreateCategoryReqDto,
        session: HttpSession,
    ): CategoryResDto {
        authenticationReader.requireAdmin(session)
        val name = TitleNormalizer.normalize(reqDto.name)
        if (name.isEmpty() || categoryRepository.existsByName(name)) throw duplicate()

        val saved =
            try {
                categoryRepository.saveAndFlush(CategoryJpaEntity(name))
            } catch (e: DataIntegrityViolationException) {
                throw duplicate()
            }
        return CategoryResDto(requireNotNull(saved.id), saved.name)
    }

    private fun duplicate() = ExpectedException("이미 존재하는 분류입니다.", HttpStatus.CONFLICT)
}
