package io.github.wntopia.gikipedia.server.domain.article.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.web.multipart.MultipartFile

data class CreateArticleReqDto(
    @field:NotBlank(message = "제목은 비어 있을 수 없습니다.")
    @field:Size(max = 255, message = "제목은 255자를 초과할 수 없습니다.")
    val title: String,
    @field:NotBlank(message = "내용은 비어 있을 수 없습니다.")
    val content: String,
    val image: MultipartFile? = null,
)
