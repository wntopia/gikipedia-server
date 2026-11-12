package io.github.wntopia.gikipedia.server.domain.article.repository

import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleJpaEntity
import io.github.wntopia.gikipedia.server.global.config.JpaAuditingConfig
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest

@DataJpaTest
@Import(JpaAuditingConfig::class)
class ArticleRepositoryViewCountIntegrationTest {
    @Autowired
    private lateinit var articleRepository: ArticleRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    private fun article(title: String) = articleRepository.saveAndFlush(ArticleJpaEntity(title = title, content = "x"))

    @Test
    fun `벌크 증가는 조회수만 늘리고 수정 시각은 건드리지 않는다`() {
        val saved = article("A")
        val id = requireNotNull(saved.id)
        // 메모리의 Instant는 나노초, DB에서 읽은 값은 마이크로초라 비교 기준도 DB에서 읽은 값으로 맞춘다.
        entityManager.clear()
        val updatedAtBefore = articleRepository.findById(id).get().updatedAt
        entityManager.clear()

        assertThat(articleRepository.addViewCount(id, 3)).isEqualTo(1)
        assertThat(articleRepository.addViewCount(id, 2)).isEqualTo(1)
        entityManager.clear()

        assertThat(articleRepository.findViewCountById(id)).isEqualTo(5)
        assertThat(articleRepository.findById(id).get().updatedAt).isEqualTo(updatedAtBefore)
    }

    @Test
    fun `문서를 수정해 저장해도 반영된 조회수를 오래된 값으로 덮어쓰지 않는다`() {
        val saved = article("A")
        val id = requireNotNull(saved.id)
        entityManager.clear()
        val stale = articleRepository.findById(id).get() // viewCount = 0으로 로드

        articleRepository.addViewCount(id, 9) // 그 사이 조회수가 반영됨
        stale.update("수정", null)
        articleRepository.saveAndFlush(stale)
        entityManager.clear()

        assertThat(articleRepository.findViewCountById(id)).isEqualTo(9)
    }

    @Test
    fun `인기 문서는 조회수 내림차순이고 같으면 id 순이다`() {
        val low = requireNotNull(article("낮음").id)
        val high = requireNotNull(article("높음").id)
        val tieFirst = requireNotNull(article("동점1").id)
        val tieSecond = requireNotNull(article("동점2").id)
        articleRepository.addViewCount(low, 1)
        articleRepository.addViewCount(high, 10)
        articleRepository.addViewCount(tieFirst, 5)
        articleRepository.addViewCount(tieSecond, 5)
        entityManager.clear()

        val popular = articleRepository.findPopular(PageRequest.of(0, 3))

        assertThat(popular.map { it.title }).containsExactly("높음", "동점1", "동점2")
        assertThat(popular.first().viewCount).isEqualTo(10)
    }
}
