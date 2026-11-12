package io.github.wntopia.gikipedia.server.domain.article.controller

import io.github.wntopia.gikipedia.server.domain.article.dto.request.SearchArticleReqDto
import io.github.wntopia.gikipedia.server.domain.article.dto.response.ArticleSearchResDto
import io.github.wntopia.gikipedia.server.domain.article.service.SearchArticleService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import team.themoment.sdk.exception.ExpectedException

class ArticleSearchControllerTest {
    private val service = mock<SearchArticleService>()
    private val controller = ArticleSearchController(service)

    @Nested
    @DisplayName("Describe: search")
    inner class DescribeSearch {
        @Nested
        @DisplayName("Context: 검색 서비스가 결과를 반환하면")
        inner class ContextSuccessfulSearch {
            @Test
            @DisplayName("It: 요청을 그대로 전달하고 서비스 결과를 반환한다")
            fun returnsServiceResult() {
                // Given
                val request = SearchArticleReqDto("  급식 안내  ", 2, 10)
                val response = ArticleSearchResDto(emptyList(), 2, 10, 0, 0)
                whenever(service.execute(request)).thenReturn(response)

                // When
                val result = controller.search(request)

                // Then
                assertThat(result).isSameAs(response)
                verify(service).execute(request)
            }
        }

        @Nested
        @DisplayName("Context: 검색 서비스가 과부하 예외를 던지면")
        inner class ContextUnavailableSearch {
            @Test
            @DisplayName("It: 예외를 빈 성공 결과로 바꾸지 않고 전파한다")
            fun propagatesUnavailableFailure() {
                // Given
                val request = SearchArticleReqDto("급식")
                val failure = ExpectedException("문서 검색을 일시적으로 사용할 수 없습니다.", HttpStatus.SERVICE_UNAVAILABLE)
                whenever(service.execute(request)).thenThrow(failure)

                // When / Then
                assertThatThrownBy { controller.search(request) }.isSameAs(failure)
            }
        }
    }

    @Nested
    @DisplayName("Describe: invalidInput")
    inner class DescribeInvalidInput {
        @Nested
        @DisplayName("Context: 입력 오류 응답을 생성하면")
        inner class ContextInvalidInput {
            @Test
            @DisplayName("It: 검색을 실행하지 않고 공통 400 오류 본문을 만든다")
            fun buildsBadRequestResponse() {
                // When
                val response = controller.invalidInput()

                // Then
                assertThat(response.status).isEqualTo(HttpStatus.BAD_REQUEST)
                assertThat(response.code).isEqualTo(400)
                assertThat(response.message).isEqualTo("검색어와 페이지 입력 값을 확인해 주세요.")
                verifyNoInteractions(service)
            }
        }
    }
}
