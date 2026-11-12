package io.github.wntopia.gikipedia.server.domain.user.dto.response

import io.github.wntopia.gikipedia.server.domain.user.entity.ContributionType
import io.github.wntopia.gikipedia.server.domain.user.repository.ContributionView
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

data class ContributionResDto(
    @field:Schema(description = "문서 ID", example = "1")
    val articleId: Long,
    @field:Schema(description = "문서 제목")
    val title: String,
    @field:Schema(description = "기여 구분 (CREATE: 문서 생성, EDIT: 문서 수정)")
    val type: ContributionType,
    @field:Schema(description = "기여 시각 (ISO-8601, UTC)")
    val contributedAt: Instant,
) {
    companion object {
        fun from(view: ContributionView): ContributionResDto =
            ContributionResDto(view.articleId, view.title, view.type, view.contributedAt)
    }
}
