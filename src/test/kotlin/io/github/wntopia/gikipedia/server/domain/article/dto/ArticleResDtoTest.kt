package io.github.wntopia.gikipedia.server.domain.article.dto

import io.github.wntopia.gikipedia.server.domain.article.dto.response.ArticleResDto
import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleJpaEntity
import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleMongoEntity
import io.github.wntopia.gikipedia.server.domain.category.dto.response.CategoryResDto
import io.github.wntopia.gikipedia.server.domain.category.entity.CategoryJpaEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.test.util.ReflectionTestUtils
import java.time.Instant

class ArticleResDtoTest {
    @Test
    @DisplayName("MySQL 엔티티로 만든 응답에 분류가 담긴다")
    fun fromJpaEntityIncludesCategory() {
        val category = CategoryJpaEntity("학교").also { ReflectionTestUtils.setField(it, "id", 3L) }
        val entity =
            ArticleJpaEntity(title = "제목", content = "내용").also {
                ReflectionTestUtils.setField(it, "id", 1L)
                it.changeCategory(category)
            }

        assertThat(ArticleResDto.from(entity).category).isEqualTo(CategoryResDto(3L, "학교"))
    }

    @Test
    @DisplayName("Mongo 읽기 뷰로 만든 응답에도 복사해 둔 분류가 담긴다")
    fun fromMongoEntityIncludesCategory() {
        val withCategory = mongoView(categoryId = 3L, categoryName = "학교")
        val withoutCategory = mongoView(categoryId = null, categoryName = null)

        assertThat(ArticleResDto.from(withCategory).category).isEqualTo(CategoryResDto(3L, "학교"))
        assertThat(ArticleResDto.from(withoutCategory).category).isNull()
    }

    /**
     * 생성자 대신 mock으로 만든다 — 다른 PR(#32 검색)이 Mongo 뷰에 필수 필드를 추가해도 이 테스트가 깨지지 않게 하기 위함이다.
     * `from()`이 읽는 값만 채운다.
     */
    private fun mongoView(
        categoryId: Long?,
        categoryName: String?,
    ): ArticleMongoEntity =
        mock<ArticleMongoEntity>().also {
            val now = Instant.now()
            whenever(it.documentId).thenReturn(1L)
            whenever(it.title).thenReturn("제목")
            whenever(it.content).thenReturn("내용")
            whenever(it.imageUrl).thenReturn(null)
            whenever(it.articleCreatedAt).thenReturn(now)
            whenever(it.articleUpdatedAt).thenReturn(now)
            whenever(it.categoryId).thenReturn(categoryId)
            whenever(it.categoryName).thenReturn(categoryName)
        }
}
