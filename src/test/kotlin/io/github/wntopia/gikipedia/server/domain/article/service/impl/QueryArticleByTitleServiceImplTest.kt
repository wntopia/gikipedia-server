package io.github.wntopia.gikipedia.server.domain.article.service.impl

import io.github.wntopia.gikipedia.server.domain.article.dto.response.ArticleResDto
import io.github.wntopia.gikipedia.server.domain.article.entity.ArticleJpaEntity
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.domain.article.service.QueryArticleService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.test.util.ReflectionTestUtils
import team.themoment.sdk.exception.ExpectedException

class QueryArticleByTitleServiceImplTest {
    private val articleRepository = mock<ArticleRepository>()
    private val queryArticleService = mock<QueryArticleService>()
    private val service = QueryArticleByTitleServiceImpl(articleRepository, queryArticleService)

    @Test
    @DisplayName("정규화한 제목으로 문서를 찾아 id 기반 조회에 위임한다")
    fun delegatesToQueryByIdWithNormalizedTitle() {
        val entity = ArticleJpaEntity("기숙사 생활 안내", "본문").also { ReflectionTestUtils.setField(it, "id", 7L) }
        val response = ArticleResDto(7L, "기숙사 생활 안내", "본문", null, null, null)
        whenever(articleRepository.findByTitle("기숙사 생활 안내")).thenReturn(entity)
        whenever(queryArticleService.execute(7L)).thenReturn(response)

        val result = service.execute("  기숙사   생활 안내 ")

        assertThat(result).isEqualTo(response)
        verify(queryArticleService).execute(7L)
    }

    @Test
    @DisplayName("없는 제목이면 404를 던진다")
    fun throwsNotFoundWhenTitleMissing() {
        whenever(articleRepository.findByTitle("없는 문서")).thenReturn(null)

        assertThatThrownBy { service.execute("없는 문서") }
            .isInstanceOfSatisfying(ExpectedException::class.java) {
                assertThat(it.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
            }
    }

    @Test
    @DisplayName("정규화한 제목으로 못 찾으면 정규화 이전에 저장된 원문 제목으로 한 번 더 찾는다")
    fun fallsBackToRawTitleForLegacyArticles() {
        val legacy = ArticleJpaEntity("기숙사  안내", "본문").also { ReflectionTestUtils.setField(it, "id", 9L) }
        val response = ArticleResDto(9L, "기숙사  안내", "본문", null, null, null)
        whenever(articleRepository.findByTitle("기숙사 안내")).thenReturn(null)
        whenever(articleRepository.findByTitle("기숙사  안내")).thenReturn(legacy)
        whenever(queryArticleService.execute(9L)).thenReturn(response)

        assertThat(service.execute("기숙사  안내")).isEqualTo(response)
    }

    @Test
    @DisplayName("터무니없이 긴 제목은 DB를 조회하지 않고 404")
    fun rejectsOverlongTitleWithoutQuery() {
        assertThatThrownBy { service.execute("가".repeat(1000)) }
            .isInstanceOfSatisfying(ExpectedException::class.java) {
                assertThat(it.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
            }
        verify(articleRepository, never()).findByTitle(any())
    }
}
