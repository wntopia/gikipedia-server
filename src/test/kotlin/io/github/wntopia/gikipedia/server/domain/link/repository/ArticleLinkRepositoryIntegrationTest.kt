package io.github.wntopia.gikipedia.server.domain.link.repository

import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleJpaEntity
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.domain.link.entity.ArticleLinkJpaEntity
import io.github.wntopia.gikipedia.server.global.config.JpaAuditingConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest

@DataJpaTest
@Import(JpaAuditingConfig::class)
class ArticleLinkRepositoryIntegrationTest {
    @Autowired
    private lateinit var articleLinkRepository: ArticleLinkRepository

    @Autowired
    private lateinit var articleRepository: ArticleRepository

    private fun article(title: String) =
        requireNotNull(articleRepository.saveAndFlush(ArticleJpaEntity(title = title, content = "x")).id)

    @Test
    fun `대상 제목을 링크한 문서를 제목 순으로 조회하고 없는 문서의 제목도 조회된다`() {
        val b = article("B 문서")
        val a = article("A 문서")
        article("C 문서")
        articleLinkRepository.save(ArticleLinkJpaEntity(b, "미작성 문서", "미작성 문서"))
        articleLinkRepository.save(ArticleLinkJpaEntity(a, "미작성 문서", "미작성 문서"))
        articleLinkRepository.save(ArticleLinkJpaEntity(a, "다른 문서", "다른 문서"))
        articleLinkRepository.flush()

        val result = articleLinkRepository.findBacklinks("미작성 문서", PageRequest.of(0, 10))

        assertThat(result.content.map { it.title }).containsExactly("A 문서", "B 문서")
        assertThat(result.totalElements).isEqualTo(2)
    }

    @Test
    fun `링크한 문서가 없으면 빈 결과다`() {
        assertThat(articleLinkRepository.findBacklinks("아무도 안 링크", PageRequest.of(0, 10)).content).isEmpty()
    }

    @Test
    fun `삭제된 키만 지우고 나머지 링크는 남긴다`() {
        val a = article("A 문서")
        articleLinkRepository.save(ArticleLinkJpaEntity(a, "x", "x"))
        articleLinkRepository.save(ArticleLinkJpaEntity(a, "y", "y"))
        articleLinkRepository.flush()

        articleLinkRepository.deleteBySourceArticleIdAndTargetKeyIn(a, listOf("x"))

        assertThat(articleLinkRepository.findTargetKeysBySourceArticleId(a)).containsExactly("y")
    }
}
