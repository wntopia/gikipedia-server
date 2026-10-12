package io.github.wntopia.gikipedia.server.domain.link.service.impl

import io.github.wntopia.gikipedia.server.domain.link.dto.response.BacklinkResDto
import io.github.wntopia.gikipedia.server.domain.link.repository.ArticleLinkRepository
import io.github.wntopia.gikipedia.server.domain.link.service.QueryBacklinksService
import io.github.wntopia.gikipedia.server.global.dto.PageResDto
import io.github.wntopia.gikipedia.server.global.text.TitleNormalizer
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class QueryBacklinksServiceImpl(
    private val articleLinkRepository: ArticleLinkRepository,
) : QueryBacklinksService {
    /** 대상 제목의 문서가 없어도 오류 없이 조회된다 — 링크만 된 미작성 문서의 "이 문서를 링크한 문서"를 보여주기 위함. */
    @Transactional(readOnly = true)
    override fun execute(
        title: String,
        page: Int,
        size: Int,
    ): PageResDto<BacklinkResDto> {
        val result = articleLinkRepository.findBacklinks(TitleNormalizer.keyOf(title), PageRequest.of(page, size))
        return PageResDto(
            items = result.content.map(BacklinkResDto::from),
            page = page,
            size = size,
            totalElements = result.totalElements,
            hasNext = result.hasNext(),
        )
    }
}
