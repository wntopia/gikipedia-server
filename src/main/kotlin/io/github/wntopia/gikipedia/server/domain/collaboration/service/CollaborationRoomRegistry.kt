package io.github.wntopia.gikipedia.server.domain.collaboration.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.wntopia.gikipedia.server.domain.collaboration.model.CollaborationRoom
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * article 단위 공동편집 방(room) 레지스트리. 단일 인스턴스 배포 기준 in-memory 저장소다.
 */
@Component
class CollaborationRoomRegistry(
    private val objectMapper: ObjectMapper,
) {
    private val rooms = ConcurrentHashMap<Long, CollaborationRoom>()

    fun getOrCreate(articleId: Long): CollaborationRoom =
        rooms.computeIfAbsent(articleId) {
            CollaborationRoom(it, objectMapper)
        }

    fun findIfActive(articleId: Long): CollaborationRoom? = rooms[articleId]

    /** 안전망 저장 완료 후 room을 정리한다. 그 사이 재입장으로 교체된 room이면(레퍼런스 불일치) 건드리지 않는다. */
    fun removeIfEmpty(room: CollaborationRoom) {
        rooms.remove(room.articleId, room)
    }
}
