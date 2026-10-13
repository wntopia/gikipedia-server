package io.github.wntopia.gikipedia.server.domain.article.dto.request

import io.github.wntopia.gikipedia.server.domain.history.dto.EditMetadata
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.web.multipart.MultipartFile

/** title은 생성 시점에만 정해지고 수정할 수 없으므로 이 요청에는 포함하지 않는다. */
data class UpdateArticleReqDto(
    @field:Schema(description = "수정할 문서 본문 (기존 본문을 통째로 교체)")
    @field:NotBlank(message = "내용은 비어 있을 수 없습니다.")
    val content: String,
    @field:Schema(description = "교체할 대표 이미지 파일 (선택, 생략 시 기존 이미지 유지, 최대 10MB)")
    val image: MultipartFile? = null,
    @field:Schema(description = "편집 요약 (선택, 최대 500자). 편집 역사에 표시된다.", example = "오탈자 수정")
    @field:Size(max = EditMetadata.MAX_SUMMARY_LENGTH, message = "편집 요약은 500자를 초과할 수 없습니다.")
    val summary: String? = null,
    @field:Schema(description = "가벼운 수정 여부 (선택, 기본 false)", example = "false")
    val minorEdit: Boolean = false,
    @field:Schema(description = "바꿀 문서 분류 ID (선택, 생략하면 기존 분류 유지). 분류를 비우는 기능은 지원하지 않는다.", example = "1")
    val categoryId: Long? = null,
)
