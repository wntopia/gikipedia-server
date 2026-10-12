package io.github.wntopia.gikipedia.server.domain.report.service.impl

import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.domain.history.repository.ArticleHistoryRepository
import io.github.wntopia.gikipedia.server.domain.report.dto.request.CreateReportReqDto
import io.github.wntopia.gikipedia.server.domain.report.entity.ArticleReportJpaEntity
import io.github.wntopia.gikipedia.server.domain.report.entity.ReportStatus
import io.github.wntopia.gikipedia.server.domain.report.repository.ArticleReportRepository
import io.github.wntopia.gikipedia.server.domain.report.service.CreateReportService
import io.github.wntopia.gikipedia.server.global.security.session.AuthenticationReader
import jakarta.servlet.http.HttpSession
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import team.themoment.sdk.exception.ExpectedException

@Service
class CreateReportServiceImpl(
    private val authenticationReader: AuthenticationReader,
    private val articleRepository: ArticleRepository,
    private val articleHistoryRepository: ArticleHistoryRepository,
    private val articleReportRepository: ArticleReportRepository,
) : CreateReportService {
    /**
     * 로그인한 사용자만 신고할 수 있다. 같은 사용자가 같은 문서에 아직 처리되지 않은 신고를 이미 냈다면 중복으로 보고
     * 409를 반환한다 — 처리된 뒤에는 다시 신고할 수 있다.
     */
    @Transactional
    override fun execute(
        articleId: Long,
        reqDto: CreateReportReqDto,
        session: HttpSession,
    ) {
        val reporterUserId =
            authenticationReader.getCurrentUser(session)?.id
                ?: throw ExpectedException("인증이 필요합니다.", HttpStatus.UNAUTHORIZED)
        if (!articleRepository.existsById(articleId)) {
            throw ExpectedException("존재하지 않는 게시글입니다.", HttpStatus.NOT_FOUND)
        }
        reqDto.revision?.let { validateRevision(articleId, it) }
        if (articleReportRepository.existsByReporterUserIdAndArticleIdAndStatus(
                reporterUserId,
                articleId,
                ReportStatus.OPEN,
            )
        ) {
            throw ExpectedException("이미 신고한 문서입니다. 처리된 뒤에 다시 신고할 수 있습니다.", HttpStatus.CONFLICT)
        }

        articleReportRepository.save(
            ArticleReportJpaEntity(reporterUserId, articleId, reqDto.revision, reqDto.reason.trim()),
        )
    }

    /** 한 번도 수정되지 않은 문서는 히스토리 행이 없지만 "생성 시점 그대로"인 리비전 1은 존재한다. */
    private fun validateRevision(
        articleId: Long,
        revision: Int,
    ) {
        val latest = articleHistoryRepository.findTopByArticleIdOrderByRevisionDesc(articleId)?.revision ?: 1
        if (revision > latest) throw ExpectedException("존재하지 않는 리비전입니다.", HttpStatus.BAD_REQUEST)
    }
}
