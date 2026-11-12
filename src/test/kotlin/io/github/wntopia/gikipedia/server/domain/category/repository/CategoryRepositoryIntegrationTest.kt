package io.github.wntopia.gikipedia.server.domain.category.repository

import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleJpaEntity
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.domain.category.entity.CategoryJpaEntity
import io.github.wntopia.gikipedia.server.global.config.JpaAuditingConfig
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.hibernate.Hibernate
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest

@DataJpaTest
@Import(JpaAuditingConfig::class)
class CategoryRepositoryIntegrationTest {
    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    @Autowired
    private lateinit var articleRepository: ArticleRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    private fun article(
        title: String,
        category: CategoryJpaEntity?,
    ) = articleRepository.saveAndFlush(
        ArticleJpaEntity(title = title, content = "x").also { it.changeCategory(category) },
    )

    @Test
    fun `분류 이름은 중복될 수 없다`() {
        categoryRepository.saveAndFlush(CategoryJpaEntity("학교"))

        assertThatThrownBy { categoryRepository.saveAndFlush(CategoryJpaEntity("학교")) }
            .isInstanceOf(DataIntegrityViolationException::class.java)
    }

    @Test
    fun `분류 목록은 이름 순이고 문서가 없는 분류도 0으로 나온다`() {
        val school = categoryRepository.saveAndFlush(CategoryJpaEntity("학교"))
        categoryRepository.saveAndFlush(CategoryJpaEntity("동아리"))
        article("A", school)
        article("B", school)
        article("분류 없음", null)

        val list = categoryRepository.findAllWithArticleCount()

        assertThat(list.map { it.name }).containsExactly("동아리", "학교")
        assertThat(list.map { it.articleCount }).containsExactly(0L, 2L)
    }

    @Test
    fun `분류별 문서 목록은 해당 분류 문서만 최근 수정 순으로 가져온다`() {
        val school = categoryRepository.saveAndFlush(CategoryJpaEntity("학교"))
        val club = categoryRepository.saveAndFlush(CategoryJpaEntity("동아리"))
        article("학교 문서 1", school)
        val latest = article("학교 문서 2", school)
        article("동아리 문서", club)

        val page = articleRepository.findByCategory(requireNotNull(school.id), PageRequest.of(0, 10))

        assertThat(page.content.map { it.title }).containsExactly("학교 문서 2", "학교 문서 1")
        assertThat(page.content.first().id).isEqualTo(latest.id)
        assertThat(page.totalElements).isEqualTo(2)
    }

    @Test
    fun `fetch join 조회는 세션 밖에서도 분류 이름을 읽을 수 있다`() {
        val school = categoryRepository.saveAndFlush(CategoryJpaEntity("학교"))
        val saved = article("분류 문서", school)
        entityManager.clear()

        val loaded = requireNotNull(articleRepository.findByIdWithCategory(requireNotNull(saved.id)))
        entityManager.clear() // 세션이 닫힌 것과 같은 상태(분리됨)

        assertThat(Hibernate.isInitialized(loaded.category)).isTrue()
        assertThat(loaded.category?.name).isEqualTo("학교")
        assertThat(articleRepository.findByIdWithCategory(Long.MAX_VALUE)).isNull()
    }
}
