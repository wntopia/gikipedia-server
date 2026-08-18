package io.github.wntopia.gikipedia.server.domain.collaboration.model

/**
 * article 공동편집 WebSocket에서 오가는 유일한 메시지 포맷.
 *
 * CRDT(Yjs) 바이트는 opaque하게 base64([dataB64])로만 다루고 백엔드는 내용을 절대 해석하지 않는다. 타입
 * 종류가 8개로 적고 안정적이라, 폴리모픽 역직렬화(`@JsonTypeInfo` 등) 대신 플랫 DTO + [type] 필드 분기로
 * 다룬다.
 */
data class CollaborationMessage(
    val type: String,
    /** opaque CRDT 바이트: update/awareness/sync(전송)/bootstrap(수신, Redis 상태 있을 때) */
    val dataB64: String? = null,
    /** save 요청의 plain text, sync의 plain text, bootstrap/external-update의 현재 본문 */
    val content: String? = null,
    /** external-update 전용 */
    val imageUrl: String? = null,
    /** saved ack */
    val revision: Int? = null,
    /** save의 저장 사유(manual/auto) — 관측용, 로직에 영향 없음 */
    val reason: String? = null,
    /** error 메시지 */
    val message: String? = null,
) {
    companion object {
        const val TYPE_BOOTSTRAP = "bootstrap"
        const val TYPE_UPDATE = "update"
        const val TYPE_AWARENESS = "awareness"
        const val TYPE_SYNC = "sync"
        const val TYPE_SAVE = "save"
        const val TYPE_SAVED = "saved"
        const val TYPE_EXTERNAL_UPDATE = "external-update"
        const val TYPE_ERROR = "error"
    }
}
