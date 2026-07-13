package io.github.wntopia.gikipedia.server.domain.article.controller

import io.github.wntopia.gikipedia.server.domain.article.dto.request.CreateArticleReqDto
import io.github.wntopia.gikipedia.server.domain.article.dto.request.UpdateArticleReqDto
import io.github.wntopia.gikipedia.server.domain.article.dto.response.ArticleResDto
import io.github.wntopia.gikipedia.server.domain.article.service.CreateArticleService
import io.github.wntopia.gikipedia.server.domain.article.service.QueryArticleService
import io.github.wntopia.gikipedia.server.domain.article.service.UpdateArticleService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import team.themoment.sdk.response.CommonApiResponse

@RestController
@RequestMapping("/api/v1/articles")
class ArticleController(
    private val createArticleService: CreateArticleService,
    private val queryArticleService: QueryArticleService,
    private val updateArticleService: UpdateArticleService,
) {
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun create(
        @ModelAttribute reqDto: CreateArticleReqDto,
    ): CommonApiResponse<ArticleResDto> = CommonApiResponse.created("", createArticleService.execute(reqDto))

    @GetMapping("/{articleId}")
    fun query(
        @PathVariable articleId: Long,
    ): ArticleResDto = queryArticleService.execute(articleId)

    @PutMapping("/{articleId}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun update(
        @PathVariable articleId: Long,
        @ModelAttribute reqDto: UpdateArticleReqDto,
    ): ArticleResDto = updateArticleService.execute(articleId, reqDto)
}
