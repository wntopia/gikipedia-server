package io.github.wntopia.gikipedia.server.domain.article.service.impl

import io.github.wntopia.gikipedia.server.domain.article.dto.request.SearchArticleReqDto
import io.github.wntopia.gikipedia.server.domain.article.dto.response.ArticleSearchResDto
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleSearchDocument
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleSearchRepository
import io.github.wntopia.gikipedia.server.global.config.ArticleSearchEnvironment
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Assertions.assertTimeoutPreemptively
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.dao.DataAccessResourceFailureException
import org.springframework.dao.InvalidDataAccessApiUsageException
import org.springframework.dao.QueryTimeoutException
import org.springframework.http.HttpStatus
import team.themoment.sdk.exception.ExpectedException
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class SearchArticleServiceImplTest {
    private val repository = mock<ArticleSearchRepository>()
    private val service = SearchArticleServiceImpl(repository, ArticleSearchEnvironment())

    @Test
    fun `동시 검색 4개가 실행 중이면 추가 요청은 DB 호출 없이 즉시 거부하고 완료 후 다시 허용한다`() {
        val entered = CountDownLatch(4)
        val release = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(4)
        whenever(repository.count("진행")).thenAnswer {
            entered.countDown()
            check(release.await(5, TimeUnit.SECONDS))
            1L
        }
        whenever(repository.search(any(), any(), any())).thenReturn(emptyList())
        try {
            val running =
                (1..4).map {
                    executor.submit<ArticleSearchResDto> { service.execute(SearchArticleReqDto("진행")) }
                }
            assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue()
            assertTimeoutPreemptively(Duration.ofSeconds(1)) {
                repeat(3) {
                    assertThatThrownBy { service.execute(SearchArticleReqDto("초과")) }
                        .isInstanceOfSatisfying(ExpectedException::class.java) {
                            assertThat(it.statusCode).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                        }
                }
            }
            verify(repository, never()).count("초과")
            verify(repository, never()).search("초과", 0, 20)
            release.countDown()
            running.forEach { assertThat(it.get(5, TimeUnit.SECONDS).totalElements).isEqualTo(1) }
            assertThat(service.execute(SearchArticleReqDto("회복")).items).isEmpty()
            verify(repository).count("회복")
        } finally {
            release.countDown()
            executor.shutdownNow()
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue()
        }
    }

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

    @Test
    fun `설정한 상한은 목록 조회까지 유지하고 잘못된 입력은 한도를 소비하지 않는다`() {
        val limitedService = SearchArticleServiceImpl(repository, ArticleSearchEnvironment(maxConcurrentRequests = 1))
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor()
        whenever(repository.search(any(), any(), any())).thenReturn(emptyList())
        whenever(repository.search("진행", 0, 20)).thenAnswer {
            entered.countDown()
            check(release.await(5, TimeUnit.SECONDS))
            emptyList<ArticleSearchDocument>()
        }
        try {
            val running = executor.submit<ArticleSearchResDto> { limitedService.execute(SearchArticleReqDto("진행")) }
            assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue()
            assertThatThrownBy { limitedService.execute(SearchArticleReqDto(" ")) }
                .isInstanceOfSatisfying(ExpectedException::class.java) {
                    assertThat(it.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
                }
            assertTimeoutPreemptively(Duration.ofSeconds(1)) {
                repeat(3) {
                    assertThatThrownBy { limitedService.execute(SearchArticleReqDto("초과")) }
                        .isInstanceOfSatisfying(ExpectedException::class.java) {
                            assertThat(it.statusCode).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                        }
                }
            }
            verify(repository, never()).count("초과")
            release.countDown()
            running.get(5, TimeUnit.SECONDS)
            assertThat(limitedService.execute(SearchArticleReqDto("회복")).items).isEmpty()
        } finally {
            release.countDown()
            executor.shutdownNow()
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue()
        }
    }

    @TestFactory
    fun `건수 또는 목록 조회 예외가 나도 동시 실행 허가를 반환한다`() =
        listOf("count", "search").flatMap { stage ->
            listOf(
                DataAccessResourceFailureException("connection"),
                QueryTimeoutException("timeout"),
                InvalidDataAccessApiUsageException("bug"),
            ).map { failure ->
                dynamicTest("$stage ${failure.javaClass.simpleName}") {
                    val failedRepository = mock<ArticleSearchRepository>()
                    val limitedService =
                        SearchArticleServiceImpl(failedRepository, ArticleSearchEnvironment(maxConcurrentRequests = 1))
                    whenever(failedRepository.search(any(), any(), any())).thenReturn(emptyList())
                    if (stage == "count") {
                        whenever(failedRepository.count("실패")).thenThrow(failure)
                    } else {
                        whenever(failedRepository.search("실패", 0, 20)).thenThrow(failure)
                    }
                    val expectedType =
                        if (failure is InvalidDataAccessApiUsageException) {
                            failure.javaClass
                        } else {
                            ExpectedException::class.java
                        }
                    assertThatThrownBy { limitedService.execute(SearchArticleReqDto("실패")) }.isInstanceOf(expectedType)
                    assertThat(limitedService.execute(SearchArticleReqDto("회복")).items).isEmpty()
                    verify(failedRepository).search("회복", 0, 20)
                }
            }
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
                assertThatThrownBy {
                    SearchArticleServiceImpl(
                        unusedRepository,
                        ArticleSearchEnvironment(),
                    ).execute(request)
                }.isInstanceOfSatisfying(ExpectedException::class.java) {
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
