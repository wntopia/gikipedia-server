package io.github.wntopia.gikipedia.server.domain.article.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

data class LookupArticleTitlesReqDto(
    @field:Schema(
        description = "존재 여부를 확인할 제목 목록 (최대 200개). 공백·유니코드 정규화는 서버가 처리한다.",
        example = "[\"기숙사 생활 안내\", \"없는 문서\"]",
    )
    @field:NotEmpty(message = "제목 목록은 비어 있을 수 없습니다.")
    @field:Size(max = MAX_TITLES, message = "제목은 한 번에 200개까지 조회할 수 있습니다.")
    val titles: List<String>,
) {
    companion object {
        const val MAX_TITLES = 200
    }
}
