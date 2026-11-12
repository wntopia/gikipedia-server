package io.github.wntopia.gikipedia.server.domain.favorite.repository

import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleJpaEntity
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.domain.favorite.entity.ArticleFavoriteJpaEntity
import io.github.wntopia.gikipedia.server.global.config.JpaAuditingConfig
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest

@DataJpaTest
@Import(JpaAuditingConfig::class)
class ArticleFavoriteRepositoryIntegrationTest {
    @Autowired
    private lateinit var favoriteRepository: ArticleFavoriteRepository

    @Autowired
    private lateinit var articleRepository: ArticleRepository

    private fun article(title: String) =
        requireNotNull(articleRepository.saveAndFlush(ArticleJpaEntity(title = title, content = "x")).id)

    @Test
    fun `같은 사용자가 같은 문서를 두 번 넣으면 유니크 제약이 막는다`() {
        val a = article("A")
        favoriteRepository.saveAndFlush(ArticleFavoriteJpaEntity(10L, a))

        assertThatThrownBy { favoriteRepository.saveAndFlush(ArticleFavoriteJpaEntity(10L, a)) }
            .isInstanceOf(DataIntegrityViolationException::class.java)
    }

    @Test
    fun `내 목록은 내 즐겨찾기만 최근 추가 순으로 제목과 함께 내려준다`() {
        val a = article("A 문서")
        val b = article("B 문서")
        favoriteRepository.save(ArticleFavoriteJpaEntity(10L, a))
        favoriteRepository.save(ArticleFavoriteJpaEntity(10L, b))
        favoriteRepository.save(ArticleFavoriteJpaEntity(99L, a))
        favoriteRepository.flush()

        val page = favoriteRepository.findByUserId(10L, PageRequest.of(0, 10))

        assertThat(page.content.map { it.title }).containsExactly("B 문서", "A 문서")
        assertThat(page.totalElements).isEqualTo(2)
    }

    @Test
    fun `삭제는 해당 사용자의 해당 문서만 지운다`() {
        val a = article("A")
        favoriteRepository.save(ArticleFavoriteJpaEntity(10L, a))
        favoriteRepository.save(ArticleFavoriteJpaEntity(99L, a))
        favoriteRepository.flush()

        assertThat(favoriteRepository.deleteByUserIdAndArticleId(10L, a)).isEqualTo(1)

        assertThat(favoriteRepository.existsByUserIdAndArticleId(10L, a)).isFalse()
        assertThat(favoriteRepository.existsByUserIdAndArticleId(99L, a)).isTrue()
        assertThat(favoriteRepository.deleteByUserIdAndArticleId(10L, a)).isEqualTo(0)
    }
}
