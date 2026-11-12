package io.github.wntopia.gikipedia.server.domain.article.service.impl

import io.github.wntopia.gikipedia.server.domain.collaboration.service.CollaborationRoomRegistry
import io.github.wntopia.gikipedia.server.global.security.session.EditorIdentity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.web.socket.WebSocketSession
import tools.jackson.module.kotlin.jacksonObjectMapper

class QueryArticleEditorsServiceImplTest {
    private val registry = CollaborationRoomRegistry(jacksonObjectMapper())
    private val service = QueryArticleEditorsServiceImpl(registry)

    private fun fakeSession(id: String) = mock<WebSocketSession>().also { whenever(it.id).thenReturn(id) }

    @Test
    @DisplayName("누군가 편집 중이면 편집자 이름과 인원 수를 반환한다")
    fun returnsEditorsWhileEditing() {
        registry.joinOrCreate(1L, fakeSession("a"), EditorIdentity(1L, "2412 홍길동", "홍길동"))
        registry.joinOrCreate(1L, fakeSession("b"), EditorIdentity(2L, "2413 김철수", "김철수"))

        val result = service.execute(1L)

        assertThat(result.editing).isTrue()
        assertThat(result.editorCount).isEqualTo(2)
        assertThat(result.editorNames).containsExactly("홍길동", "김철수")
    }

    @Test
    @DisplayName("이름을 모르면 라벨로 대신한다")
    fun fallsBackToLabel() {
        registry.joinOrCreate(1L, fakeSession("a"), EditorIdentity(1L, "2412 홍길동"))

        assertThat(service.execute(1L).editorNames).containsExactly("2412 홍길동")
    }

    @Test
    @DisplayName("마지막 편집자가 나가면 결과가 비어 있다")
    fun emptyAfterLastEditorLeaves() {
        val room = registry.joinOrCreate(1L, fakeSession("a"), EditorIdentity(1L, "2412 홍길동", "홍길동"))
        room.leave("a")

        val result = service.execute(1L)

        assertThat(result.editing).isFalse()
        assertThat(result.editorCount).isEqualTo(0)
        assertThat(result.editorNames).isEmpty()
    }

    @Test
    @DisplayName("활성 room이 없는 문서도 오류 없이 빈 결과를 반환한다")
    fun emptyWhenNoRoom() {
        assertThat(service.execute(404L).editing).isFalse()
        assertThat(service.execute(404L).editorNames).isEmpty()
    }
}
