package io.github.wntopia.gikipedia.server.domain.article.controller

import io.github.wntopia.gikipedia.server.domain.article.dto.request.CreateArticleReqDto
import io.github.wntopia.gikipedia.server.domain.article.dto.request.UpdateArticleImageReqDto
import io.github.wntopia.gikipedia.server.domain.article.dto.request.UpdateArticleReqDto
import io.github.wntopia.gikipedia.server.domain.article.dto.response.ArticleResDto
import io.github.wntopia.gikipedia.server.domain.article.service.CreateArticleService
import io.github.wntopia.gikipedia.server.domain.article.service.QueryArticleService
import io.github.wntopia.gikipedia.server.domain.article.service.UpdateArticleImageService
import io.github.wntopia.gikipedia.server.domain.article.service.UpdateArticleService
import jakarta.servlet.http.HttpSession
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PatchMapping
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
    private val updateArticleImageService: UpdateArticleImageService,
) {
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun create(
        @Valid @ModelAttribute reqDto: CreateArticleReqDto,
    ): CommonApiResponse<ArticleResDto> = CommonApiResponse.created("", createArticleService.execute(reqDto))

    @GetMapping("/{articleId}")
    fun query(
        @PathVariable articleId: Long,
    ): ArticleResDto = queryArticleService.execute(articleId)

    @PutMapping("/{articleId}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun update(
        @PathVariable articleId: Long,
        @Valid @ModelAttribute reqDto: UpdateArticleReqDto,
        session: HttpSession,
    ): ArticleResDto = updateArticleService.execute(articleId, reqDto, session)

    /** 실시간 공동편집 중에도 대표 이미지만 별도로 교체할 수 있는 전용 엔드포인트. content는 건드리지 않는다. */
    @PatchMapping("/{articleId}/image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun updateImage(
        @PathVariable articleId: Long,
        @Valid @ModelAttribute reqDto: UpdateArticleImageReqDto,
        session: HttpSession,
    ): ArticleResDto = updateArticleImageService.execute(articleId, reqDto, session)
}
