package io.github.wntopia.gikipedia.server.domain.link.service

import io.github.wntopia.gikipedia.server.domain.link.repository.ArticleLinkRepository
import io.github.wntopia.gikipedia.server.domain.link.repository.BacklinkView
import io.github.wntopia.gikipedia.server.domain.link.service.impl.QueryBacklinksServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

class QueryBacklinksServiceImplTest {
    private val repository = mock<ArticleLinkRepository>()
    private val service = QueryBacklinksServiceImpl(repository)

    private fun view(
        id: Long,
        title: String,
    ) = object : BacklinkView {
        override val id = id
        override val title = title
    }

    @Test
    @DisplayName("제목을 정규화(대소문자 무시)한 키로 조회하고 페이지 정보를 채운다")
    fun queriesByNormalizedKey() {
        whenever(repository.findBacklinks(eq("home page"), any())).thenReturn(
            PageImpl(listOf(view(1L, "A")), PageRequest.of(0, 1), 3),
        )

        val result = service.execute("  Home   Page ", 0, 1)

        assertThat(result.items.map { it.title }).containsExactly("A")
        assertThat(result.totalElements).isEqualTo(3)
        assertThat(result.hasNext).isTrue()
    }
}
