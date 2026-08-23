package io.github.wntopia.gikipedia.server.domain.collaboration.service

import io.github.wntopia.gikipedia.server.domain.collaboration.model.CollaborationRoom
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketSession
import tools.jackson.databind.ObjectMapper
import java.util.concurrent.ConcurrentHashMap

/**
 * article 단위 공동편집 방(room) 레지스트리. 단일 인스턴스 배포 기준 in-memory 저장소다.
 */
@Component
class CollaborationRoomRegistry(
    private val objectMapper: ObjectMapper,
) {
    private val rooms = ConcurrentHashMap<Long, CollaborationRoom>()

    /**
     * room을 찾아 세션을 join시키고, 없으면 새로 만들어 join시킨다.
     *
     * join까지 [ConcurrentHashMap.compute]의 리매핑 함수 안에서 원자적으로 수행한다 — 그래야 안전망 정리
     * 스레드가 [removeIfEmpty]에서 같은 키에 대해 수행하는 "여전히 비어있는지" 확인과 완전히 직렬화되어,
     * "정리 스레드가 비어있다고 확인한 직후, 막 재접속한 세션이 join하기 직전"에 room이 registry에서
     * 지워져버리는 경쟁 상태(TOCTOU)가 생기지 않는다. 두 메서드 모두 같은 key에 대해 호출되는 한
     * [ConcurrentHashMap]이 상호 배제를 보장한다.
     */
    fun joinOrCreate(
        articleId: Long,
        session: WebSocketSession,
    ): CollaborationRoom =
        rooms.compute(articleId) { _, existing ->
            val room = existing ?: CollaborationRoom(articleId, objectMapper)
            room.cancelPendingSafetyNet()
            room.join(session)
            room
        }!!

    fun findIfActive(articleId: Long): CollaborationRoom? = rooms[articleId]

    /**
     * 안전망 저장 완료 후 room을 정리한다.
     *
     * 넘겨준 room이 registry의 현재 room과 같은 레퍼런스이면서(그 사이 재입장으로 교체되지 않았고) 여전히
     * 비어있을 때만(그 사이 [joinOrCreate]로 세션이 다시 join하지 않았을 때만) 제거한다. 이 최종 확인을
     * [joinOrCreate]와 같은 [ConcurrentHashMap.computeIfPresent] 경로로 수행해 두 메서드가 절대 교차
     * 실행되지 않도록 한다.
     */
    fun removeIfEmpty(room: CollaborationRoom) {
        rooms.computeIfPresent(room.articleId) { _, current ->
            if (current === room && current.isEmpty()) null else current
        }
    }
}
