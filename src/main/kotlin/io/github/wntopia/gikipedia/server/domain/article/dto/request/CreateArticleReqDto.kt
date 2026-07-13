package io.github.wntopia.gikipedia.server.domain.article.dto.request

import org.springframework.web.multipart.MultipartFile

data class CreateArticleReqDto(
    val title: String,
    val content: String,
    val image: MultipartFile? = null,
)
