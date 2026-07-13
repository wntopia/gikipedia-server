package io.github.wntopia.gikipedia.server.domain.article.service

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils
import team.themoment.sdk.exception.ExpectedException

@Component
class ArticleValidator {
    fun validate(
        title: String,
        content: String,
    ) {
        if (!StringUtils.hasText(title)) throw ExpectedException("제목은 비어 있을 수 없습니다.", HttpStatus.BAD_REQUEST)
        if (title.length > 255) throw ExpectedException("제목은 255자를 초과할 수 없습니다.", HttpStatus.BAD_REQUEST)
        if (!StringUtils.hasText(content)) throw ExpectedException("내용은 비어 있을 수 없습니다.", HttpStatus.BAD_REQUEST)
    }
}
