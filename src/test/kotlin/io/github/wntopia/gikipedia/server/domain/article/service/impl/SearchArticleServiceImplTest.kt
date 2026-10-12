package io.github.wntopia.gikipedia.server.domain.article.service.impl

import io.github.wntopia.gikipedia.server.domain.article.dto.request.SearchArticleReqDto
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleSearchDocument
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleSearchRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.dao.DataAccessResourceFailureException
import org.springframework.dao.InvalidDataAccessApiUsageException
import org.springframework.dao.QueryTimeoutException
import org.springframework.http.HttpStatus
import team.themoment.sdk.exception.ExpectedException
import java.time.Instant

class SearchArticleServiceImplTest {
    private val repository = mock<ArticleSearchRepository>()
    private val service = SearchArticleServiceImpl(repository)

    @Test
    fun `양끝 공백만 제거하고 원본 수정 시각과 전체 페이지를 반환한다`() {
        val updated = Instant.parse("2026-10-12T00:00:00Z")
        whenever(repository.count("급식 안내")).thenReturn(41)
        whenever(repository.search("급식 안내", 20, 20)).thenReturn(
            listOf(ArticleSearchDocument(7, "제목", "학교 급식 안내입니다", updated)),
        )
        val result = service.execute(SearchArticleReqDto("  급식 안내  ", 1, 20))
        assertThat(result.totalPages).isEqualTo(3)
        assertThat(result.totalElements).isEqualTo(41)
        assertThat(result.page).isEqualTo(1)
        assertThat(result.items.single().updatedAt).isEqualTo(updated)
        assertThat(result.items.single().id).isEqualTo(7)
        assertThat(
            result.items
                .single()
                .snippet
                .filter { it.highlight }
                .single()
                .text,
        ).isEqualTo("급식 안내")
    }

    @TestFactory
    fun `잘못된 입력은 DB에 보내지 않는다`() =
        listOf(
            SearchArticleReqDto(),
            SearchArticleReqDto(" \u00a0\t"),
            SearchArticleReqDto("가".repeat(101)),
            SearchArticleReqDto("🍱".repeat(101)),
            SearchArticleReqDto("a\u0000b"),
            SearchArticleReqDto("급식", -1),
            SearchArticleReqDto("급식", size = 0),
            SearchArticleReqDto("급식", size = 51),
        ).map { request ->
            dynamicTest(request.toString()) {
                val unusedRepository = mock<ArticleSearchRepository>()
                assertThatThrownBy { SearchArticleServiceImpl(unusedRepository).execute(request) }
                    .isInstanceOfSatisfying(ExpectedException::class.java) {
                        assertThat(it.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
                    }
                verifyNoInteractions(unusedRepository)
            }
        }

    @Test
    fun `최대 페이지의 offset은 Long으로 계산한다`() {
        whenever(repository.search(any(), any(), any())).thenReturn(emptyList())
        val result = service.execute(SearchArticleReqDto("🍱".repeat(100), Int.MAX_VALUE, 50))
        verify(repository).search("🍱".repeat(100), Int.MAX_VALUE.toLong() * 50, 50)
        assertThat(result.items).isEmpty()
        assertThat(result.totalPages).isZero()
    }

    @Test
    fun `범위 밖 페이지도 전체 건수를 보존한다`() {
        whenever(repository.count("급식")).thenReturn(4)
        whenever(repository.search(any(), any(), any())).thenReturn(emptyList())
        val result = service.execute(SearchArticleReqDto("급식", 10))
        assertThat(result.items).isEmpty()
        assertThat(result.page).isEqualTo(10)
        assertThat(result.totalPages).isEqualTo(1)
        assertThat(result.totalElements).isEqualTo(4)
    }

    @Test
    fun `검색 저장소 연결 실패와 시간 초과만 503으로 변환한다`() {
        whenever(repository.count("연결")).thenThrow(DataAccessResourceFailureException("internal host"))
        whenever(repository.count("시간")).thenReturn(1)
        whenever(repository.search("시간", 0, 20)).thenThrow(QueryTimeoutException("internal query"))
        for (query in listOf("연결", "시간")) {
            assertThatThrownBy { service.execute(SearchArticleReqDto(query)) }
                .isInstanceOfSatisfying(ExpectedException::class.java) {
                    assertThat(it.statusCode).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                    assertThat(it.message).doesNotContain("internal")
                }
        }
        whenever(repository.count("버그")).thenThrow(InvalidDataAccessApiUsageException("bug"))
        assertThatThrownBy { service.execute(SearchArticleReqDto("버그")) }
            .isInstanceOf(InvalidDataAccessApiUsageException::class.java)
    }
}
