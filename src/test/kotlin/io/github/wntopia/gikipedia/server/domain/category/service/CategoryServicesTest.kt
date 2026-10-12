package io.github.wntopia.gikipedia.server.domain.category.service

import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.domain.category.dto.request.CreateCategoryReqDto
import io.github.wntopia.gikipedia.server.domain.category.entity.CategoryJpaEntity
import io.github.wntopia.gikipedia.server.domain.category.repository.CategoryRepository
import io.github.wntopia.gikipedia.server.domain.category.service.impl.CreateCategoryServiceImpl
import io.github.wntopia.gikipedia.server.domain.category.service.impl.QueryCategoryArticlesServiceImpl
import io.github.wntopia.gikipedia.server.global.security.session.AuthenticationReader
import jakarta.servlet.http.HttpSession
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.test.util.ReflectionTestUtils
import team.themoment.sdk.exception.ExpectedException

class CategoryServicesTest {
    private val authenticationReader = mock<AuthenticationReader>()
    private val categoryRepository = mock<CategoryRepository>()
    private val articleRepository = mock<ArticleRepository>()
    private val session = mock<HttpSession>()

    private val create = CreateCategoryServiceImpl(authenticationReader, categoryRepository)
    private val queryArticles = QueryCategoryArticlesServiceImpl(categoryRepository, articleRepository)

    private fun status(e: Throwable) = (e as ExpectedException).statusCode

    @Test
    @DisplayName("관리자가 이름을 다듬어 분류를 만든다")
    fun createsCategoryAsAdmin() {
        whenever(categoryRepository.existsByName("동아리")).thenReturn(false)
        whenever(categoryRepository.saveAndFlush(any<CategoryJpaEntity>())).thenAnswer {
            it.getArgument<CategoryJpaEntity>(0).also { e -> ReflectionTestUtils.setField(e, "id", 5L) }
        }

        val result = create.execute(CreateCategoryReqDto("  동아리  "), session)

        verify(authenticationReader).requireAdmin(session)
        assertThat(result.id).isEqualTo(5L)
        assertThat(result.name).isEqualTo("동아리")
    }

    @Test
    @DisplayName("관리자가 아니면 분류를 만들 수 없고 아무것도 저장하지 않는다")
    fun rejectsNonAdmin() {
        doThrow(
            ExpectedException("관리자만 접근할 수 있습니다.", HttpStatus.FORBIDDEN),
        ).whenever(authenticationReader).requireAdmin(session)

        assertThatThrownBy { create.execute(CreateCategoryReqDto("동아리"), session) }
            .satisfies({ assertThat(status(it)).isEqualTo(HttpStatus.FORBIDDEN) })
        verify(categoryRepository, never()).saveAndFlush(any<CategoryJpaEntity>())
    }

    @Test
    @DisplayName("이미 있는 이름이면 409, 동시 생성으로 제약에 걸려도 409")
    fun rejectsDuplicateName() {
        whenever(categoryRepository.existsByName("학교")).thenReturn(true)
        assertThatThrownBy { create.execute(CreateCategoryReqDto("학교"), session) }
            .satisfies({ assertThat(status(it)).isEqualTo(HttpStatus.CONFLICT) })

        whenever(categoryRepository.existsByName("행사")).thenReturn(false)
        whenever(
            categoryRepository.saveAndFlush(any<CategoryJpaEntity>()),
        ).thenThrow(DataIntegrityViolationException("dup"))
        assertThatThrownBy { create.execute(CreateCategoryReqDto("행사"), session) }
            .satisfies({ assertThat(status(it)).isEqualTo(HttpStatus.CONFLICT) })
    }

    @Test
    @DisplayName("없는 분류의 문서 목록은 404")
    fun unknownCategoryArticles() {
        whenever(categoryRepository.existsById(9L)).thenReturn(false)

        assertThatThrownBy { queryArticles.execute(9L, 0, 20) }
            .satisfies({ assertThat(status(it)).isEqualTo(HttpStatus.NOT_FOUND) })
    }
}
