package io.github.wntopia.gikipedia.server.domain.history.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import java.time.Instant

/**
 * 특정 리비전 시점으로 재구성된 문서 응답.
 *
 * content는 스냅샷 + diff 순차 적용으로 복원한 결과다. 과거 리비전은 불변이므로 이 DTO는 그대로 캐시된다([Serializable] 구현). title은 현재 diff 추적 대상이 아니어서 문서의 현재 제목을
 * 싣는다.
 */
data class ArticleRevisionResDto(
    @field:Schema(description = "문서 ID", example = "1")
    val articleId: Long,
    @field:Schema(description = "리비전 번호", example = "3")
    val revision: Int,
    @field:Schema(description = "문서 제목 (항상 현재 제목)")
    val title: String,
    @field:Schema(description = "해당 리비전 시점의 본문")
    val content: String,
    @field:Schema(description = "해당 리비전의 편집자 (\"학번 이름\")", example = "2412 홍길동")
    val editor: String,
    @field:Schema(description = "해당 리비전의 편집 시각 (ISO-8601, UTC)")
    val editedAt: Instant?,
    /** 이 리비전이 문서의 현재 최신인지 여부. 최신 리비전은 이후 수정으로 바뀔 수 있어 캐시하지 않는다(캐시 unless 판정용). */
    @field:Schema(description = "이 리비전이 현재 최신 버전인지 여부")
    val latest: Boolean,
) : Serializable
