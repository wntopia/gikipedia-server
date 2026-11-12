package io.github.wntopia.gikipedia.server.domain.report.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateReportReqDto(
    @field:Schema(description = "신고 사유 (최대 500자, 앞뒤 공백은 제거된다)", example = "근거 없는 비방 문구가 추가되었습니다.")
    @field:NotBlank(message = "신고 사유는 비어 있을 수 없습니다.")
    @field:Size(max = 500, message = "신고 사유는 500자를 초과할 수 없습니다.")
    val reason: String,
)
