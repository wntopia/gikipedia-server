package io.github.wntopia.gikipedia.server.domain.user.service

import io.github.wntopia.gikipedia.server.domain.auth.dto.response.DataGsmUserInfoResDto
import io.github.wntopia.gikipedia.server.domain.user.entity.UserJpaEntity
import io.github.wntopia.gikipedia.server.domain.user.repository.UserRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/** 로그인한 계정의 사용자 레코드를 만들거나(첫 로그인 = 가입) 최신 정보로 갱신한다. */
@Component
class UserRegistrar(
    private val userRepository: UserRepository,
) {
    /** @return 사용자 레코드. 계정 id가 없는 비정상 응답이면 null. */
    @Transactional
    fun register(userInfo: DataGsmUserInfoResDto): UserJpaEntity? {
        val datagsmUserId = userInfo.id ?: return null
        val name = userInfo.student?.name
        val existing = userRepository.findByDatagsmUserId(datagsmUserId)
        if (existing != null) {
            existing.refresh(name, userInfo.role)
            return existing
        }
        return try {
            userRepository.saveAndFlush(UserJpaEntity(datagsmUserId, name, userInfo.role))
        } catch (e: DataIntegrityViolationException) {
            // 같은 계정의 동시 첫 로그인 — 먼저 들어온 쪽의 행을 쓴다.
            userRepository.findByDatagsmUserId(datagsmUserId)
        }
    }
}
