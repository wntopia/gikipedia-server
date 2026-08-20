package io.github.wntopia.gikipedia.server.domain.history.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.github.wntopia.gikipedia.server.domain.history.dto.ArticleHistorySegmentEntry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant

/** encode(JSON+gzip) → decode 라운드트립이 원본 데이터를 정확히 복원하는지 검증한다. */
class ArticleHistorySegmentCodecTest {
    private val objectMapper = ObjectMapper().registerKotlinModule().registerModule(JavaTimeModule())
    private val codec = ArticleHistorySegmentCodec(objectMapper)

    @Test
    @DisplayName("여러 리비전을 인코딩 후 디코딩하면 원본과 완전히 동일하고 revision 오름차순으로 정렬된다")
    fun roundTrip() {
        val entries =
            listOf(
                ArticleHistorySegmentEntry(3, "2412 홍길동", "diff-3", Instant.parse("2026-01-03T00:00:00Z")),
                ArticleHistorySegmentEntry(
                    2,
                    "2412 김철수",
                    "diff-2\n특수문자 !@#\n개행 포함",
                    Instant.parse("2026-01-02T00:00:00Z"),
                ),
            )

        val decoded = codec.decode(codec.encode(entries))

        assertThat(decoded).containsExactly(entries[1], entries[0])
    }

    @Test
    @DisplayName("빈 리스트도 인코딩/디코딩이 가능하다")
    fun emptyList() {
        val decoded = codec.decode(codec.encode(emptyList<ArticleHistorySegmentEntry>()))

        assertThat(decoded).isEmpty()
    }
}
