package io.github.wntopia.gikipedia.server.domain.article.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

data class ArticleSearchItemResDto(
    @field:Schema(description = "문서 ID", example = "1")
    val id: Long,
    @field:Schema(description = "문서 제목")
    val title: String,
    @field:Schema(description = "표시용 본문 발췌와 강조 구간. 본문이 비어 있으면 빈 배열")
    val snippet: List<SnippetSegmentResDto>,
    @field:Schema(description = "원본 문서의 최종 수정 시각 (ISO-8601, UTC)")
    val updatedAt: Instant,
)
