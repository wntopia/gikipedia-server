package io.github.wntopia.gikipedia.server.domain.article.dto

import io.github.wntopia.gikipedia.server.domain.article.dto.response.ArticleResDto
import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleJpaEntity
import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleMongoEntity
import io.github.wntopia.gikipedia.server.domain.category.dto.response.CategoryResDto
import io.github.wntopia.gikipedia.server.domain.category.entity.CategoryJpaEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
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
        val now = Instant.now()
        val withCategory = ArticleMongoEntity(1L, "제목", "내용", null, now, now, 2, 3L, "학교")
        val withoutCategory = ArticleMongoEntity(1L, "제목", "내용", null, now, now, 2)

        assertThat(ArticleResDto.from(withCategory).category).isEqualTo(CategoryResDto(3L, "학교"))
        assertThat(ArticleResDto.from(withoutCategory).category).isNull()
    }
}
