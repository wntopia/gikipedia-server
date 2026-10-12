package io.github.wntopia.gikipedia.server.domain.article.service.impl

import io.github.wntopia.gikipedia.server.domain.article.dto.response.RecentChangeResDto
import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.domain.article.service.QueryRecentChangesService
import io.github.wntopia.gikipedia.server.domain.user.entity.ContributionType
import io.github.wntopia.gikipedia.server.domain.user.repository.ContributionRepository
import io.github.wntopia.gikipedia.server.domain.user.repository.UserRepository
import io.github.wntopia.gikipedia.server.global.dto.PageResDto
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

/**
 * 문서 단위로 묶은 최근 변경 목록. 순서와 페이지는 문서의 마지막 수정 시각이 정하고, 마지막 기여자와 오늘의 편집 횟수는
 * 기여 원장([io.github.wntopia.gikipedia.server.domain.user.entity.ContributionJpaEntity])에서 목록에 나온 문서들만 한 번씩 조회해 채운다.
 */
@Service
class QueryRecentChangesServiceImpl(
    private val articleRepository: ArticleRepository,
    private val contributionRepository: ContributionRepository,
    private val userRepository: UserRepository,
    private val clock: Clock,
) : QueryRecentChangesService {
    @Transactional(readOnly = true)
    override fun execute(
        page: Int,
        size: Int,
    ): PageResDto<RecentChangeResDto> {
        val articles = articleRepository.findRecentlyUpdated(PageRequest.of(page, size))
        val articleIds = articles.content.map { it.id }
        if (articleIds.isEmpty()) {
            return PageResDto(emptyList(), page, size, articles.totalElements, false)
        }

        val startOfToday = LocalDate.now(clock.withZone(KST)).atStartOfDay(KST).toInstant()
        val statsByArticle =
            contributionRepository
                .findEditStatsSince(
                    articleIds,
                    startOfToday,
                ).associateBy { it.articleId }
        val latestByArticle = contributionRepository.findLatestByArticleIds(articleIds).associateBy { it.articleId }
        val namesByUserId =
            userRepository
                .findByDatagsmUserIdIn(latestByArticle.values.mapNotNull { it.userId }.toSet())
                .associate { it.datagsmUserId to it.name }

        val items =
            articles.content.map { article ->
                val latest = latestByArticle[article.id]
                val stats = statsByArticle[article.id]
                RecentChangeResDto(
                    articleId = article.id,
                    title = article.title,
                    lastEditorName = latest?.userId?.let { namesByUserId[it] },
                    updatedAt = article.updatedAt,
                    newlyCreated = latest?.type == ContributionType.CREATE,
                    todayEditCount = stats?.editCount ?: 0,
                    todayEditorCount = stats?.editorCount ?: 0,
                )
            }
        return PageResDto(items, page, size, articles.totalElements, articles.hasNext())
    }

    companion object {
        private val KST: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
