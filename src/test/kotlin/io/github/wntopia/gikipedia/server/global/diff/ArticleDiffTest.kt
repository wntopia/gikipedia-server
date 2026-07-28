package io.github.wntopia.gikipedia.server.global.diff

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class ArticleDiffTest {
    private val articleDiff = ArticleDiff()

    @Test
    @DisplayName("생성한 diff를 원본에 적용하면 수정본이 복원된다 (round-trip)")
    fun roundTrip() {
        val before = "첫째 줄\n둘째 줄\n셋째 줄"
        val after = "첫째 줄\n수정된 둘째 줄\n셋째 줄\n넷째 줄"

        val diff = articleDiff.generate(before, after)

        assertThat(articleDiff.apply(before, diff)).isEqualTo(after)
    }

    @Test
    @DisplayName("여러 diff를 순차 forward 적용하면 최종 리비전이 복원된다")
    fun sequentialApply() {
        val rev1 = "A\nB\nC"
        val rev2 = "A\nB2\nC"
        val rev3 = "A\nB2\nC\nD"

        val diff12 = articleDiff.generate(rev1, rev2)
        val diff23 = articleDiff.generate(rev2, rev3)

        val reconstructed = articleDiff.apply(articleDiff.apply(rev1, diff12), diff23)

        assertThat(reconstructed).isEqualTo(rev3)
    }

    @Test
    @DisplayName("변경이 없으면 빈 diff를 생성하고, 빈 diff 적용은 원본을 그대로 반환한다")
    fun noChange() {
        val content = "변화 없음\n그대로"

        val diff = articleDiff.generate(content, content)

        assertThat(diff).isEmpty()
        assertThat(articleDiff.apply(content, diff)).isEqualTo(content)
    }

    @Test
    @DisplayName("여러 줄이 동시에 바뀌어도 정확히 복원된다")
    fun multiLineChange() {
        val before = (1..30).joinToString("\n") { "line $it" }
        val after =
            (1..30).joinToString("\n") { if (it % 5 == 0) "changed $it" else "line $it" } + "\nappended"

        val diff = articleDiff.generate(before, after)

        assertThat(articleDiff.apply(before, diff)).isEqualTo(after)
    }
}
