package io.github.wntopia.gikipedia.server.domain.article.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import org.springframework.web.multipart.MultipartFile

/** title은 생성 시점에만 정해지고 수정할 수 없으므로 이 요청에는 포함하지 않는다. */
data class UpdateArticleReqDto(
    @field:Schema(description = "수정할 문서 본문 (기존 본문을 통째로 교체)")
    @field:NotBlank(message = "내용은 비어 있을 수 없습니다.")
    val content: String,
    @field:Schema(description = "교체할 대표 이미지 파일 (선택, 생략 시 기존 이미지 유지, 최대 10MB)")
    val image: MultipartFile? = null,
)
