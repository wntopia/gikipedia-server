package io.github.wntopia.gikipedia.server.domain.link.service

import io.github.wntopia.gikipedia.server.domain.link.entity.ArticleLinkJpaEntity
import io.github.wntopia.gikipedia.server.domain.link.repository.ArticleLinkRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ArticleLinkIndexerTest {
    private val repository = mock<ArticleLinkRepository>()
    private val indexer = ArticleLinkIndexer(repository)

    @Test
    @DisplayName("새로 생긴 링크만 추가한다")
    fun addsNewLinks() {
        whenever(repository.findTargetKeysBySourceArticleId(1L)).thenReturn(listOf("기숙사"))

        indexer.reindex(1L, "내 문서", "[[기숙사]] [[동아리]]")

        val captor = argumentCaptor<Collection<ArticleLinkJpaEntity>>()
        verify(repository).saveAll(captor.capture())
        assertThat(captor.firstValue.map { it.targetTitle }).containsExactly("동아리")
        verify(repository, never()).deleteBySourceArticleIdAndTargetKeyIn(any(), any())
    }

    @Test
    @DisplayName("본문에서 지운 링크는 관계에서도 지운다")
    fun removesDroppedLinks() {
        whenever(repository.findTargetKeysBySourceArticleId(1L)).thenReturn(listOf("기숙사", "동아리"))

        indexer.reindex(1L, "내 문서", "[[기숙사]]")

        verify(repository).deleteBySourceArticleIdAndTargetKeyIn(1L, setOf("동아리"))
        verify(repository, never()).saveAll(any<Collection<ArticleLinkJpaEntity>>())
    }

    @Test
    @DisplayName("자기 자신을 링크한 것은 관계로 남기지 않는다")
    fun ignoresSelfLink() {
        whenever(repository.findTargetKeysBySourceArticleId(1L)).thenReturn(emptyList())

        indexer.reindex(1L, "내 문서", "[[내 문서]]")

        verify(repository, never()).saveAll(any<Collection<ArticleLinkJpaEntity>>())
    }

    @Test
    @DisplayName("링크 대소문자만 바뀐 경우 변경으로 보지 않는다")
    fun caseOnlyChangeIsNoop() {
        whenever(repository.findTargetKeysBySourceArticleId(1L)).thenReturn(listOf("home"))

        indexer.reindex(1L, "내 문서", "[[HOME]]")

        verify(repository, never()).saveAll(any<Collection<ArticleLinkJpaEntity>>())
        verify(repository, never()).deleteBySourceArticleIdAndTargetKeyIn(any(), any())
    }
}
