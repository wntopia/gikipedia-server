package io.github.wntopia.gikipedia.server.domain.article.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.web.multipart.MultipartFile

data class CreateArticleReqDto(
    @field:Schema(description = "문서 제목 (최대 255자, 생성 후 수정 불가)", example = "기숙사 생활 안내")
    @field:NotBlank(message = "제목은 비어 있을 수 없습니다.")
    @field:Size(max = 255, message = "제목은 255자를 초과할 수 없습니다.")
    val title: String,
    @field:Schema(description = "문서 본문")
    @field:NotBlank(message = "내용은 비어 있을 수 없습니다.")
    val content: String,
    @field:Schema(description = "대표 이미지 파일 (선택, 최대 10MB)")
    val image: MultipartFile? = null,
)
