package io.github.wntopia.gikipedia.server.domain.article.service.impl

import io.github.wntopia.gikipedia.server.domain.article.dto.request.LookupArticleTitlesReqDto
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleTitleProjection
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class LookupArticleTitlesServiceImplTest {
    private val articleRepository = mock<ArticleRepository>()
    private val service = LookupArticleTitlesServiceImpl(articleRepository)

    private fun projection(
        id: Long,
        title: String,
    ) = object : ArticleTitleProjection {
        override val id = id
        override val title = title
    }

    @Test
    @DisplayName("요청 순서대로 제목별 존재 여부와 id를 반환한다")
    fun returnsExistenceInRequestOrder() {
        whenever(articleRepository.findIdAndTitleByTitleIn(any())).thenReturn(listOf(projection(3L, "기숙사 생활 안내")))

        val result = service.execute(LookupArticleTitlesReqDto(listOf("없는 문서", " 기숙사  생활 안내 ")))

        assertThat(result.items.map { it.exists }).containsExactly(false, true)
        assertThat(result.items.map { it.id }).containsExactly(null, 3L)
        assertThat(result.items[1].title).isEqualTo(" 기숙사  생활 안내 ")
    }

    @Test
    @DisplayName("대소문자만 다른 제목도 같은 문서로 해석한다")
    fun matchesIgnoringCase() {
        whenever(articleRepository.findIdAndTitleByTitleIn(any())).thenReturn(listOf(projection(5L, "Gikipedia")))

        val result = service.execute(LookupArticleTitlesReqDto(listOf("gikipedia")))

        assertThat(result.items.single().id).isEqualTo(5L)
    }

    @Test
    @DisplayName("공백뿐인 제목만 오면 DB를 조회하지 않고 모두 없음으로 답한다")
    fun skipsQueryForBlankTitles() {
        val result = service.execute(LookupArticleTitlesReqDto(listOf("   ")))

        assertThat(result.items.single().exists).isFalse()
        verify(articleRepository, never()).findIdAndTitleByTitleIn(any())
    }

    @Test
    @DisplayName("정규화 이전에 저장된 원문 제목도 조회 대상에 포함하고 같은 제목의 요청에 연결한다")
    fun includesRawTitlesForLegacyArticles() {
        val captor = argumentCaptor<Collection<String>>()
        whenever(articleRepository.findIdAndTitleByTitleIn(captor.capture()))
            .thenReturn(listOf(projection(4L, " 동아리  소개")))

        val result = service.execute(LookupArticleTitlesReqDto(listOf(" 동아리  소개")))

        assertThat(captor.firstValue).contains("동아리 소개", " 동아리  소개")
        assertThat(result.items.single().id).isEqualTo(4L)
    }

    @Test
    @DisplayName("컬럼 길이를 넘는 제목은 조회하지 않고 없음으로 답한다")
    fun skipsTitlesLongerThanColumn() {
        val result = service.execute(LookupArticleTitlesReqDto(listOf("가".repeat(256))))

        assertThat(result.items.single().exists).isFalse()
        verify(articleRepository, never()).findIdAndTitleByTitleIn(any())
    }
}
