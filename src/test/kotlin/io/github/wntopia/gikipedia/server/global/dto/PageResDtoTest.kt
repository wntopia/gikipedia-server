package io.github.wntopia.gikipedia.server.global.dto

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class PageResDtoTest {
    private val all = (1..5).toList()

    @Test
    @DisplayName("중간 페이지는 요청한 구간과 다음 페이지 존재 여부를 반환한다")
    fun middlePage() {
        val page = PageResDto.sliceOf(all, 0, 2)

        assertThat(page.items).containsExactly(1, 2)
        assertThat(page.totalElements).isEqualTo(5)
        assertThat(page.hasNext).isTrue()
    }

    @Test
    @DisplayName("마지막 페이지는 남은 항목만 담고 다음 페이지가 없다")
    fun lastPage() {
        val page = PageResDto.sliceOf(all, 2, 2)

        assertThat(page.items).containsExactly(5)
        assertThat(page.hasNext).isFalse()
    }

    @Test
    @DisplayName("범위를 넘어선 페이지는 빈 목록을 반환한다")
    fun pageBeyondRange() {
        val page = PageResDto.sliceOf(all, 10, 2)

        assertThat(page.items).isEmpty()
        assertThat(page.hasNext).isFalse()
    }
}
