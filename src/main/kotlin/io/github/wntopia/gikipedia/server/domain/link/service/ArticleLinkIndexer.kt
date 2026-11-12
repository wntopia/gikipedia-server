package io.github.wntopia.gikipedia.server.domain.link.service

import io.github.wntopia.gikipedia.server.domain.link.WikiLinkExtractor
import io.github.wntopia.gikipedia.server.domain.link.entity.ArticleLinkJpaEntity
import io.github.wntopia.gikipedia.server.domain.link.repository.ArticleLinkRepository
import io.github.wntopia.gikipedia.server.global.text.TitleNormalizer
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 문서 본문의 `[[링크]]`를 `article_links`에 맞춘다. 문서 생성·수정과 같은 트랜잭션 안에서 호출해야 본문과 링크
 * 관계가 함께 커밋/롤백된다. 바뀐 링크만 추가·삭제하므로 같은 본문으로 여러 번 호출해도 결과가 같다.
 */
@Component
class ArticleLinkIndexer(
    private val articleLinkRepository: ArticleLinkRepository,
) {
    @Transactional
    fun reindex(
        sourceArticleId: Long,
        ownTitle: String,
        content: String,
    ) {
        val ownKey = TitleNormalizer.keyOf(ownTitle)
        val wanted =
            WikiLinkExtractor
                .extract(content)
                .filter {
                    TitleNormalizer.keyOf(it) != ownKey
                }.associateBy(TitleNormalizer::keyOf)
        val existing = articleLinkRepository.findTargetKeysBySourceArticleId(sourceArticleId).toSet()

        val removed = existing - wanted.keys
        if (removed.isNotEmpty()) {
            articleLinkRepository.deleteBySourceArticleIdAndTargetKeyIn(sourceArticleId, removed)
        }
        val added = wanted.filterKeys { it !in existing }
        if (added.isNotEmpty()) {
            articleLinkRepository.saveAll(
                added.map { (key, title) ->
                    ArticleLinkJpaEntity(sourceArticleId, title, key)
                },
            )
        }
    }
}
