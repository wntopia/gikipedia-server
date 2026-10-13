package io.github.wntopia.gikipedia.server.domain.category.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable

/** 문서 응답에 붙는 분류 요약. Redis 캐시에도 담기므로 [Serializable]이다. */
data class CategoryResDto(
    @field:Schema(description = "분류 ID", example = "1")
    val id: Long,
    @field:Schema(description = "분류 이름", example = "학교")
    val name: String,
) : Serializable
