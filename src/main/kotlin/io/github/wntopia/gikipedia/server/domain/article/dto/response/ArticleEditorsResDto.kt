package io.github.wntopia.gikipedia.server.domain.article.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class ArticleEditorsResDto(
    @field:Schema(description = "지금 이 문서를 편집 중인 사람이 있는지 여부")
    val editing: Boolean,
    @field:Schema(description = "편집 중인 사용자 수 (같은 사용자의 여러 탭은 한 명으로 센다)", example = "2")
    val editorCount: Int,
    @field:Schema(
        description = "편집 중인 사용자 이름 (학번 제외). \"○○ 외 N명\"은 첫 이름과 editorCount - 1로 만든다.",
        example = "[\"이서연\", \"홍길동\"]",
    )
    val editorNames: List<String>,
) {
    companion object {
        val EMPTY = ArticleEditorsResDto(false, 0, emptyList())
    }
}
