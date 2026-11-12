package io.github.wntopia.gikipedia.server.domain.favorite.service

import io.github.wntopia.gikipedia.server.domain.article.repository.ArticleRepository
import io.github.wntopia.gikipedia.server.domain.favorite.entity.ArticleFavoriteJpaEntity
import io.github.wntopia.gikipedia.server.domain.favorite.repository.ArticleFavoriteRepository
import io.github.wntopia.gikipedia.server.domain.favorite.service.impl.AddFavoriteServiceImpl
import io.github.wntopia.gikipedia.server.domain.favorite.service.impl.QueryFavoriteStatusServiceImpl
import io.github.wntopia.gikipedia.server.domain.favorite.service.impl.RemoveFavoriteServiceImpl
import io.github.wntopia.gikipedia.server.global.security.session.AuthenticationReader
import jakarta.servlet.http.HttpSession
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import team.themoment.sdk.exception.ExpectedException

class FavoriteServicesTest {
    private val authenticationReader = mock<AuthenticationReader>()
    private val articleRepository = mock<ArticleRepository>()
    private val favoriteRepository = mock<ArticleFavoriteRepository>()
    private val session = mock<HttpSession>()

    private val add = AddFavoriteServiceImpl(authenticationReader, articleRepository, favoriteRepository)
    private val remove = RemoveFavoriteServiceImpl(authenticationReader, favoriteRepository)
    private val status = QueryFavoriteStatusServiceImpl(authenticationReader, favoriteRepository)

    private fun loggedInAs(userId: Long) {
        whenever(authenticationReader.requireUserId(session)).thenReturn(userId)
        whenever(authenticationReader.getCurrentUser(session)).thenReturn(
            io.github.wntopia.gikipedia.server.domain.auth.dto.response.DataGsmUserInfoResDto(
                userId,
                null,
                "USER",
                true,
                null,
            ),
        )
    }

    private fun unauthorized() = ExpectedException("인증이 필요합니다.", HttpStatus.UNAUTHORIZED)

    @Test
    @DisplayName("로그인한 사용자가 문서를 즐겨찾기에 추가한다")
    fun addsFavorite() {
        loggedInAs(10L)
        whenever(articleRepository.existsById(1L)).thenReturn(true)
        whenever(favoriteRepository.existsByUserIdAndArticleId(10L, 1L)).thenReturn(false)
        whenever(favoriteRepository.saveAndFlush(any<ArticleFavoriteJpaEntity>())).thenAnswer {
            it.getArgument<ArticleFavoriteJpaEntity>(0)
        }

        add.execute(1L, session)

        verify(favoriteRepository).saveAndFlush(any<ArticleFavoriteJpaEntity>())
    }

    @Test
    @DisplayName("이미 추가한 문서를 다시 추가해도 한 번만 들어간다")
    fun addIsIdempotent() {
        loggedInAs(10L)
        whenever(articleRepository.existsById(1L)).thenReturn(true)
        whenever(favoriteRepository.existsByUserIdAndArticleId(10L, 1L)).thenReturn(true)

        add.execute(1L, session)

        verify(favoriteRepository, never()).saveAndFlush(any<ArticleFavoriteJpaEntity>())
    }

    @Test
    @DisplayName("동시에 두 번 추가돼 유니크 제약에 걸려도 성공으로 본다")
    fun addSurvivesConcurrentDuplicate() {
        loggedInAs(10L)
        whenever(articleRepository.existsById(1L)).thenReturn(true)
        whenever(favoriteRepository.existsByUserIdAndArticleId(10L, 1L)).thenReturn(false)
        whenever(
            favoriteRepository.saveAndFlush(any<ArticleFavoriteJpaEntity>()),
        ).thenThrow(DataIntegrityViolationException("dup"))

        add.execute(1L, session)
    }

    @Test
    @DisplayName("존재하지 않는 문서는 404, 비로그인은 401")
    fun addRejectsMissingArticleAndAnonymous() {
        loggedInAs(10L)
        whenever(articleRepository.existsById(99L)).thenReturn(false)
        assertThatThrownBy { add.execute(99L, session) }
            .isInstanceOfSatisfying(
                ExpectedException::class.java,
            ) { assertThat(it.statusCode).isEqualTo(HttpStatus.NOT_FOUND) }

        whenever(authenticationReader.requireUserId(session)).thenThrow(unauthorized())
        assertThatThrownBy { add.execute(1L, session) }
            .isInstanceOfSatisfying(
                ExpectedException::class.java,
            ) { assertThat(it.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED) }
    }

    @Test
    @DisplayName("삭제는 로그인이 필요하고, 즐겨찾기에 없던 문서도 오류 없이 끝난다")
    fun removeRequiresLoginAndIsIdempotent() {
        loggedInAs(10L)
        whenever(favoriteRepository.deleteByUserIdAndArticleId(10L, 1L)).thenReturn(0)
        remove.execute(1L, session)
        verify(favoriteRepository).deleteByUserIdAndArticleId(10L, 1L)

        whenever(authenticationReader.requireUserId(session)).thenThrow(unauthorized())
        assertThatThrownBy { remove.execute(1L, session) }.isInstanceOf(ExpectedException::class.java)
    }

    @Test
    @DisplayName("즐겨찾기 여부 조회는 비로그인이면 false를 반환한다")
    fun statusIsFalseForAnonymous() {
        whenever(authenticationReader.getCurrentUser(session)).thenReturn(null)

        assertThat(status.execute(1L, session).favorited).isFalse()
    }

    @Test
    @DisplayName("즐겨찾기 여부 조회는 로그인한 사용자의 즐겨찾기를 확인한다")
    fun statusReflectsFavorite() {
        loggedInAs(10L)
        whenever(favoriteRepository.existsByUserIdAndArticleId(10L, 1L)).thenReturn(true)

        assertThat(status.execute(1L, session).favorited).isTrue()
    }
}
