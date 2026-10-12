package io.github.wntopia.gikipedia.server.domain.user.service

import io.github.wntopia.gikipedia.server.domain.user.entity.ContributionJpaEntity
import io.github.wntopia.gikipedia.server.domain.user.entity.ContributionType
import io.github.wntopia.gikipedia.server.domain.user.repository.ContributionRepository
import org.springframework.stereotype.Component

/** 호출 측(문서 생성·수정)의 트랜잭션 안에서 기여를 한 건 기록한다. 편집자를 알 수 없으면(userId=null) 기록하지 않는다. */
@Component
class ContributionRecorder(
    private val contributionRepository: ContributionRepository,
) {
    fun recordCreate(
        userId: Long?,
        articleId: Long,
    ) {
        userId ?: return
        contributionRepository.save(ContributionJpaEntity(userId, articleId, ContributionType.CREATE))
    }

    fun recordEdit(
        userId: Long?,
        articleId: Long,
        revision: Int,
    ) {
        userId ?: return
        contributionRepository.save(ContributionJpaEntity(userId, articleId, ContributionType.EDIT, revision))
    }
}
