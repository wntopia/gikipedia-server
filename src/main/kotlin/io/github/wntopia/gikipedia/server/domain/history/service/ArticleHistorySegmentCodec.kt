package io.github.wntopia.gikipedia.server.domain.history.service

import io.github.wntopia.gikipedia.server.domain.history.dto.ArticleHistorySegmentEntry
import org.springframework.stereotype.Component
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.ObjectMapper
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * 압축 세그먼트의 페이로드(interior 리비전 목록)를 JSON으로 직렬화한 뒤 gzip으로 압축/해제한다.
 *
 * unified diff 문자열은 반복되는 라인이 많아 gzip 압축률이 좋고, JSON이라 필요하면 수동으로도
 * 디코딩해 읽을 수 있어 디버깅하기 쉽다. JDK 표준 java.util.zip만 사용하므로 별도 의존성이 필요 없다.
 */
@Component
class ArticleHistorySegmentCodec(
    private val objectMapper: ObjectMapper,
) {
    fun encode(entries: List<ArticleHistorySegmentEntry>): ByteArray {
        val sorted = entries.sortedBy { it.revision }
        val json = objectMapper.writeValueAsBytes(sorted)
        val buffer = ByteArrayOutputStream()
        GZIPOutputStream(buffer).use { it.write(json) }
        return buffer.toByteArray()
    }

    /** 반환되는 리스트는 항상 revision 오름차순이다([encode]가 정렬해서 저장하기 때문). */
    fun decode(compressedPayload: ByteArray): List<ArticleHistorySegmentEntry> {
        val json = GZIPInputStream(ByteArrayInputStream(compressedPayload)).use { it.readBytes() }
        return objectMapper.readValue(json, object : TypeReference<List<ArticleHistorySegmentEntry>>() {})
    }
}
