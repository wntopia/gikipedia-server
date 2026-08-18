package io.github.wntopia.gikipedia.server.domain.article.dto.request

import jakarta.validation.constraints.NotNull
import org.springframework.web.multipart.MultipartFile

/** 실시간 공동편집 중에도 대표 이미지만 별도로 교체할 수 있도록 하는 전용 요청. content는 건드리지 않는다. */
data class UpdateArticleImageReqDto(
    @field:NotNull(message = "이미지는 비어 있을 수 없습니다.")
    val image: MultipartFile,
)
