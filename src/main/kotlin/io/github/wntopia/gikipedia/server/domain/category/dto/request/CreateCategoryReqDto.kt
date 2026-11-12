package io.github.wntopia.gikipedia.server.domain.category.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateCategoryReqDto(
    @field:Schema(description = "분류 이름 (최대 50자, 앞뒤 공백은 제거되며 중복 불가)", example = "동아리")
    @field:NotBlank(message = "분류 이름은 비어 있을 수 없습니다.")
    @field:Size(max = 50, message = "분류 이름은 50자를 초과할 수 없습니다.")
    val name: String,
)
