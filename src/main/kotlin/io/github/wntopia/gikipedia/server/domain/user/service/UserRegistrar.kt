package io.github.wntopia.gikipedia.server.domain.user.service

import io.github.wntopia.gikipedia.server.domain.auth.dto.response.DataGsmUserInfoResDto
import io.github.wntopia.gikipedia.server.domain.user.entity.UserJpaEntity
import io.github.wntopia.gikipedia.server.domain.user.repository.UserRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component

/** 로그인한 계정의 사용자 레코드를 만들거나(첫 로그인 = 가입) 최신 정보로 갱신한다. */
@Component
class UserRegistrar(
    private val userRepository: UserRepository,
) {
    /**
     * 가입(생성)과 갱신. 일부러 `@Transactional`을 걸지 않는다 — 동시 첫 로그인으로 유니크 위반이 나면 그 예외를 여기서
     * 처리해야 하는데, 바깥 트랜잭션이 있으면 리포지토리 호출의 실패가 그 트랜잭션을 rollback-only로 만들어 catch해도
     * 커밋 시 `UnexpectedRollbackException`이 나기 때문이다. 각 리포지토리 호출이 자체 트랜잭션으로 실행된다.
     *
     * @return 사용자 레코드. 계정 id가 없는 비정상 응답이면 null.
     */
    fun register(userInfo: DataGsmUserInfoResDto): UserJpaEntity? {
        val datagsmUserId = userInfo.id ?: return null
        val name = userInfo.student?.name
        val existing = userRepository.findByDatagsmUserId(datagsmUserId)
        if (existing != null) {
            if (existing.name == name && existing.role == userInfo.role) return existing
            existing.refresh(name, userInfo.role)
            return userRepository.save(existing)
        }
        return try {
            userRepository.saveAndFlush(UserJpaEntity(datagsmUserId, name, userInfo.role))
        } catch (e: DataIntegrityViolationException) {
            // 같은 계정의 동시 첫 로그인 — 먼저 들어온 쪽의 행을 쓴다.
            userRepository.findByDatagsmUserId(datagsmUserId)
        }
    }

    /**
     * 읽기 API용. 이미 가입된 계정이면 조회만 하고(쓰기 없음), 없을 때만 [register]로 만든다. `/auth/me`처럼 앱 진입마다
     * 호출되는 API가 매번 갱신 쿼리를 내지 않게 하기 위함이다. 이름·역할 갱신은 로그인 시점([register])에 한다.
     */
    fun findOrRegister(userInfo: DataGsmUserInfoResDto): UserJpaEntity? {
        val datagsmUserId = userInfo.id ?: return null
        return userRepository.findByDatagsmUserId(datagsmUserId) ?: register(userInfo)
    }
}
