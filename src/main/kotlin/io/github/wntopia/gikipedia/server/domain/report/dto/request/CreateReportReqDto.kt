package io.github.wntopia.gikipedia.server.domain.report.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateReportReqDto(
    @field:Schema(description = "신고 사유 (최대 1000자)", example = "허위 정보가 포함되어 있습니다.")
    @field:NotBlank(message = "신고 사유는 비어 있을 수 없습니다.")
    @field:Size(max = 1000, message = "신고 사유는 1000자를 초과할 수 없습니다.")
    val reason: String,
    @field:Schema(description = "특정 리비전을 신고할 때의 리비전 번호 (선택, 생략하면 문서 전체를 신고)", example = "3")
    @field:Min(1, message = "리비전 번호는 1 이상이어야 합니다.")
    val revision: Int? = null,
)
