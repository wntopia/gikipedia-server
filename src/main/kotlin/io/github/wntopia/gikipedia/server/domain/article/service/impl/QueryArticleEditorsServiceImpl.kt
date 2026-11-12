package io.github.wntopia.gikipedia.server.domain.article.service.impl

import io.github.wntopia.gikipedia.server.domain.article.dto.response.ArticleEditorsResDto
import io.github.wntopia.gikipedia.server.domain.article.service.QueryArticleEditorsService
import io.github.wntopia.gikipedia.server.domain.collaboration.service.CollaborationRoomRegistry
import org.springframework.stereotype.Service

/** 활성 공동편집 room의 현재 참여자 기준. room이 없거나 비어 있으면(마지막 편집자가 나갔으면) 빈 결과다. */
@Service
class QueryArticleEditorsServiceImpl(
    private val collaborationRoomRegistry: CollaborationRoomRegistry,
) : QueryArticleEditorsService {
    override fun execute(articleId: Long): ArticleEditorsResDto {
        val editors = collaborationRoomRegistry.findIfActive(articleId)?.currentEditors().orEmpty()
        if (editors.isEmpty()) return ArticleEditorsResDto.EMPTY

        return ArticleEditorsResDto(
            editing = true,
            editorCount = editors.size,
            editorNames = editors.map { it.name ?: it.label },
        )
    }
}
