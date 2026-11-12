package io.github.wntopia.gikipedia.server.domain.link.dto.response

import io.github.wntopia.gikipedia.server.domain.link.repository.BacklinkView
import io.swagger.v3.oas.annotations.media.Schema

data class BacklinkResDto(
    @field:Schema(description = "링크한 문서의 ID", example = "1")
    val id: Long,
    @field:Schema(description = "링크한 문서의 제목")
    val title: String,
) {
    companion object {
        fun from(view: BacklinkView): BacklinkResDto = BacklinkResDto(view.id, view.title)
    }
}
