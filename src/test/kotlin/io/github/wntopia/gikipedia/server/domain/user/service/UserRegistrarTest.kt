package io.github.wntopia.gikipedia.server.domain.user.service

import io.github.wntopia.gikipedia.server.domain.auth.dto.response.DataGsmStudentInfoResDto
import io.github.wntopia.gikipedia.server.domain.auth.dto.response.DataGsmUserInfoResDto
import io.github.wntopia.gikipedia.server.domain.user.entity.UserJpaEntity
import io.github.wntopia.gikipedia.server.domain.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.dao.DataIntegrityViolationException

class UserRegistrarTest {
    private val userRepository = mock<UserRepository>()
    private val registrar = UserRegistrar(userRepository)

    private fun userInfo(
        id: Long?,
        name: String? = "홍길동",
    ) = DataGsmUserInfoResDto(
        id = id,
        email = "a@gsm.hs.kr",
        role = "USER",
        isStudent = true,
        student = student(name),
    )

    private fun student(name: String?) =
        DataGsmStudentInfoResDto(
            id = 1L,
            name = name,
            sex = null,
            email = null,
            grade = null,
            classNum = null,
            number = null,
            studentNumber = 2412,
            major = null,
            specialty = null,
            role = null,
            dormitoryFloor = null,
            dormitoryRoom = null,
            isLeaveSchool = null,
            majorClub = null,
            autonomousClub = null,
            githubId = null,
            githubUrl = null,
        )

    @Test
    @DisplayName("처음 로그인한 계정은 사용자 레코드를 만든다")
    fun createsUserOnFirstLogin() {
        whenever(userRepository.findByDatagsmUserId(10L)).thenReturn(null)
        whenever(userRepository.saveAndFlush(any<UserJpaEntity>())).thenAnswer { it.getArgument<UserJpaEntity>(0) }

        val user = registrar.register(userInfo(10L))

        assertThat(user?.datagsmUserId).isEqualTo(10L)
        assertThat(user?.name).isEqualTo("홍길동")
    }

    @Test
    @DisplayName("이미 있는 계정은 새로 만들지 않고 이름이 바뀐 경우에만 갱신해 저장한다")
    fun refreshesExistingUserOnlyWhenChanged() {
        val existing = UserJpaEntity(10L, "옛 이름", "USER")
        whenever(userRepository.findByDatagsmUserId(10L)).thenReturn(existing)
        whenever(userRepository.save(any<UserJpaEntity>())).thenAnswer { it.getArgument<UserJpaEntity>(0) }

        val user = registrar.register(userInfo(10L, name = "새 이름"))

        assertThat(user).isSameAs(existing)
        assertThat(existing.name).isEqualTo("새 이름")
        verify(userRepository).save(existing)
        verify(userRepository, never()).saveAndFlush(any<UserJpaEntity>())
    }

    @Test
    @DisplayName("바뀐 것이 없으면 쓰기 없이 기존 레코드를 돌려준다")
    fun skipsWriteWhenUnchanged() {
        val existing = UserJpaEntity(10L, "홍길동", "USER")
        whenever(userRepository.findByDatagsmUserId(10L)).thenReturn(existing)

        assertThat(registrar.register(userInfo(10L))).isSameAs(existing)
        verify(userRepository, never()).save(any<UserJpaEntity>())
    }

    @Test
    @DisplayName("findOrRegister는 이미 가입된 계정이면 조회만 한다")
    fun findOrRegisterReadsOnlyWhenPresent() {
        val existing = UserJpaEntity(10L, "옛 이름", "USER")
        whenever(userRepository.findByDatagsmUserId(10L)).thenReturn(existing)

        assertThat(registrar.findOrRegister(userInfo(10L, name = "새 이름"))).isSameAs(existing)
        assertThat(existing.name).isEqualTo("옛 이름")
        verify(userRepository, never()).save(any<UserJpaEntity>())
        verify(userRepository, never()).saveAndFlush(any<UserJpaEntity>())
    }

    @Test
    @DisplayName("findOrRegister는 없는 계정이면 가입시킨다")
    fun findOrRegisterCreatesWhenMissing() {
        whenever(userRepository.findByDatagsmUserId(10L)).thenReturn(null)
        whenever(userRepository.saveAndFlush(any<UserJpaEntity>())).thenAnswer { it.getArgument<UserJpaEntity>(0) }

        assertThat(registrar.findOrRegister(userInfo(10L))?.datagsmUserId).isEqualTo(10L)
    }

    @Test
    @DisplayName("계정 id가 없으면 레코드를 만들지 않는다")
    fun skipsWhenAccountIdMissing() {
        assertThat(registrar.register(userInfo(null))).isNull()
        verify(userRepository, never()).saveAndFlush(any<UserJpaEntity>())
    }

    @Test
    @DisplayName("동시 첫 로그인으로 유니크 제약에 걸리면 먼저 저장된 행을 돌려준다")
    fun returnsExistingRowOnConcurrentFirstLogin() {
        val winner = UserJpaEntity(10L, "홍길동", "USER")
        whenever(userRepository.findByDatagsmUserId(10L)).thenReturn(null, winner)
        whenever(userRepository.saveAndFlush(any<UserJpaEntity>())).thenThrow(DataIntegrityViolationException("dup"))

        assertThat(registrar.register(userInfo(10L))).isSameAs(winner)
    }
}
