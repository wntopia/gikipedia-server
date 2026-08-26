package io.github.wntopia.gikipedia.server.domain.history.dto.response

import java.io.Serializable
import java.time.Instant

/**
 * 특정 리비전 시점으로 재구성된 문서 응답.
 *
 * content는 스냅샷 + diff 순차 적용으로 복원한 결과다. 과거 리비전은 불변이므로 이 DTO는 그대로 캐시된다([Serializable] 구현). title은 현재 diff 추적 대상이 아니어서 문서의 현재 제목을
 * 싣는다.
 */
data class ArticleRevisionResDto(
    val articleId: Long,
    val revision: Int,
    val title: String,
    val content: String,
    val editor: String,
    val editedAt: Instant?,
    /** 이 리비전이 문서의 현재 최신인지 여부. 최신 리비전은 이후 수정으로 바뀔 수 있어 캐시하지 않는다(캐시 unless 판정용). */
    val latest: Boolean,
) : Serializable
