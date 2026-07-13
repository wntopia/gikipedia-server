package io.github.wntopia.gikipedia.server.domain.article.dto.request

import org.springframework.web.multipart.MultipartFile

data class UpdateArticleReqDto(
    val title: String,
    val content: String,
    val image: MultipartFile? = null,
)
