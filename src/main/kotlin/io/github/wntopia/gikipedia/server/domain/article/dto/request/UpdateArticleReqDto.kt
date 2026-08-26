package io.github.wntopia.gikipedia.server.domain.article.dto.request

import jakarta.validation.constraints.NotBlank
import org.springframework.web.multipart.MultipartFile

/** title은 생성 시점에만 정해지고 수정할 수 없으므로 이 요청에는 포함하지 않는다. */
data class UpdateArticleReqDto(
    @field:NotBlank(message = "내용은 비어 있을 수 없습니다.")
    val content: String,
    val image: MultipartFile? = null,
)
