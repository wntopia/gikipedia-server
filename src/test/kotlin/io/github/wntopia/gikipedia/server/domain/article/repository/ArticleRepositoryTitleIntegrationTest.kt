package io.github.wntopia.gikipedia.server.domain.article.repository

import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleJpaEntity
import io.github.wntopia.gikipedia.server.global.config.JpaAuditingConfig
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException

@DataJpaTest
@Import(JpaAuditingConfig::class)
class ArticleRepositoryTitleIntegrationTest {
    @Autowired
    private lateinit var articleRepository: ArticleRepository

    @Test
    fun `같은 제목을 두 번 저장하면 유니크 제약이 막는다`() {
        articleRepository.saveAndFlush(ArticleJpaEntity(title = "제목", content = "a"))

        assertThatThrownBy { articleRepository.saveAndFlush(ArticleJpaEntity(title = "제목", content = "b")) }
            .isInstanceOf(DataIntegrityViolationException::class.java)
    }

    @Test
    fun `제목 일괄 조회는 존재하는 문서의 id와 제목만 돌려준다`() {
        val saved = articleRepository.saveAndFlush(ArticleJpaEntity(title = "있는 문서", content = "a"))

        val found = articleRepository.findIdAndTitleByTitleIn(listOf("있는 문서", "없는 문서"))

        assertThat(found.map { it.title }).containsExactly("있는 문서")
        assertThat(found.single().id).isEqualTo(saved.id)
    }
}
