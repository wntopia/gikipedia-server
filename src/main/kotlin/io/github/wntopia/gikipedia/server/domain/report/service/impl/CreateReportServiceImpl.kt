package io.github.wntopia.gikipedia.server.domain.report.service.impl

import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.domain.history.repository.ArticleHistoryRepository
import io.github.wntopia.gikipedia.server.domain.report.dto.request.CreateReportReqDto
import io.github.wntopia.gikipedia.server.domain.report.entity.ArticleReportJpaEntity
import io.github.wntopia.gikipedia.server.domain.report.repository.ArticleReportRepository
import io.github.wntopia.gikipedia.server.domain.report.service.CreateReportService
import io.github.wntopia.gikipedia.server.global.security.session.AuthenticationReader
import jakarta.servlet.http.HttpSession
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import team.themoment.sdk.exception.ExpectedException

@Service
class CreateReportServiceImpl(
    private val authenticationReader: AuthenticationReader,
    private val articleRepository: ArticleRepository,
    private val articleHistoryRepository: ArticleHistoryRepository,
    private val articleReportRepository: ArticleReportRepository,
) : CreateReportService {
    /**
     * 일부러 `@Transactional`을 걸지 않는다 — 동시 중복 신고로 `saveAndFlush`가 유니크 위반을 던지면 바깥 트랜잭션이
     * rollback-only가 돼 409 대신 500이 나간다(AddFavoriteServiceImpl과 같은 이유).
     */
    override fun execute(
        articleId: Long,
        revision: Int,
        reqDto: CreateReportReqDto,
        session: HttpSession,
    ) {
        val userId = authenticationReader.requireUserId(session)
        if (!articleRepository.existsById(articleId)) {
            throw ExpectedException("존재하지 않는 게시글입니다.", HttpStatus.NOT_FOUND)
        }
        // 압축으로 중간 row가 지워져도 [1, 최신 리비전] 범위의 번호는 모두 유효하다(ReconstructArticleServiceImpl과 같은 판정).
        val latestRevision = articleHistoryRepository.findTopByArticleIdOrderByRevisionDesc(articleId)?.revision
        if (latestRevision == null || revision !in 1..latestRevision) {
            throw ExpectedException("존재하지 않는 리비전입니다.", HttpStatus.NOT_FOUND)
        }
        if (articleReportRepository.existsByUserIdAndArticleIdAndRevision(
                userId,
                articleId,
                revision,
            )
        ) {
            throw duplicated()
        }

        try {
            articleReportRepository.saveAndFlush(
                ArticleReportJpaEntity(userId, articleId, revision, reqDto.reason.trim()),
            )
        } catch (e: DataIntegrityViolationException) {
            throw duplicated()
        }
    }

    private fun duplicated() = ExpectedException("이미 신고한 리비전입니다.", HttpStatus.CONFLICT)
}
