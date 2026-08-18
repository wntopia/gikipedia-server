package io.github.wntopia.gikipedia.server.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 실시간 공동편집 관련 설정. `application.yaml`의 `collaboration` 하위에서 조정한다.
 *
 * `crdtTtlHours`는 정상 정리 경로(마지막 참여자 퇴장 + 안전망 저장 후 명시적 삭제)가 실행되지 못하는 예외
 * 상황(앱 크래시 등)에 대한 재해복구용 TTL이다. `leaveDebounceSeconds`는 마지막 참여자가 나간 뒤 안전망
 * 저장을 실행하기까지 대기하는 시간으로, 짧은 새로고침/재연결에 안전망이 불필요하게 발동하지 않도록 한다.
 */
@ConfigurationProperties(prefix = "collaboration")
data class CollaborationEnvironment(
    val crdtTtlHours: Long = 6,
    val leaveDebounceSeconds: Long = 20,
)
