package io.github.wntopia.gikipedia.server.domain.history.repository

import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleJpaEntity
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.domain.history.entity.ArticleSnapshotJpaEntity
import io.github.wntopia.gikipedia.server.global.config.JpaAuditingConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import

@DataJpaTest
@Import(JpaAuditingConfig::class)
class ArticleSnapshotRepositoryIntegrationTest {
    @Autowired
    private lateinit var snapshotRepository: ArticleSnapshotRepository

    @Autowired
    private lateinit var articleRepository: ArticleRepository

    @Test
    fun `압축된 스냅샷 중 기준보다 낮은 리비전만 내림차순으로 가져온다`() {
        val article = articleRepository.saveAndFlush(ArticleJpaEntity(title = "스냅샷 문서", content = "x"))
        listOf(1, 11, 21, 31).forEach { revision ->
            val snapshot = ArticleSnapshotJpaEntity(article, revision, "c$revision")
            if (revision != 21) snapshot.attachCompressedInteriorPayload(byteArrayOf(1))
            snapshotRepository.save(snapshot)
        }
        snapshotRepository.flush()

        assertThat(
            snapshotRepository.findCompactedRevisionsBelow(requireNotNull(article.id), 31),
        ).containsExactly(11, 1)
        assertThat(snapshotRepository.findCompactedRevisionsBelow(requireNotNull(article.id), 1)).isEmpty()
    }
}
