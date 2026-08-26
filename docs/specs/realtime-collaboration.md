# 위키 글 실시간 공동편집(WebSocket + CRDT) 백엔드 구현

## Context

지금까지 글(article) 수정은 `PUT /api/v1/articles/{id}` 한 번의 요청으로 처리되고, 동시 수정은 `SELECT ... FOR UPDATE` 비관적 락으로 직렬화되는 last-write-wins 구조였다. 여러 사람이 같은 글을 동시에 열어 편집하는 상황에서 "누가 지금 쓰고 있는지"를 보여주고, 나아가 Google Docs 수준으로 실시간 동시 편집이 가능하게 만들고 싶다는 요구다. 동시성이 이번 작업의 핵심 초점이며, WebSocket을 도입해 실시간성을 확보한다.

인터뷰를 통해 다음을 확정했다:
- 범위는 **완전 공동편집(CRDT)**. Presence만 보여주는 soft-lock 수준이 아니라 진짜 실시간 텍스트 병합.
- 알고리즘 접근은 **CRDT + 백엔드는 릴레이/영속화만** 담당. 프론트(별도 저장소, 미정 — 이번에 함께 프로토콜을 정함)가 Yjs 같은 CRDT 클라이언트로 실제 병합을 수행하고, 백엔드는 바이너리를 opaque byte로 취급한다. JVM에 안정적인 Yjs 디코더가 없다는 게 이 선택의 기술적 근거이기도 하다.
- 리비전 확정은 **명시적 저장 + 자동 안전망**(마지막 편집자 퇴장 시 디바운스 저장) 둘 다.
- CRDT 라이브 상태는 **Redis에 임시 저장**(사용자가 명시적으로 선택).
- 배포는 **단일 인스턴스** 기준이며, 멀티 인스턴스 확장은 계획하지 않는다.
- 기존 `PUT`은 유지하되 **사람이 에디터에서 편집할 때는 항상 WebSocket 경로만** 쓴다. PUT은 비대화형/외부 API 클라이언트 전용으로 개념 분리. PUT/이미지 업로드가 성공하면 활성 room에 가볍게 반영(복잡한 충돌 차단 없음).
- Presence는 **이름 + 커서 위치 + 색상**(Yjs awareness, 백엔드는 opaque 릴레이).
- **이미지(imageUrl) 변경도 실시간 반영** 필요(단, CRDT 병합 대상 아님 — last-write-wins broadcast).
- 검증은 **자동화 통합테스트(가짜 WebSocket 클라이언트) + 실제 Yjs를 쓰는 수동 스모크 페이지** 둘 다.

코드베이스 조사와 설계 검토를 거쳐 아래 접근을 확정했다. 모든 인용 시그니처(`ArticleUpdatedEvent`, `ArticleCacheStore`, `ArticleRepository.findByIdForUpdate`, `SchedulingConfig.taskScheduler()`, `CorsEnvironment`, `AuthenticationReader`, `@ApplicationModuleListener` 기반 Mongo sync 리스너)는 실제 파일을 읽어 재확인했다.

## 설계 원칙

- 백엔드는 CRDT(Yjs) 바이너리를 **절대 디코딩하지 않는다**. "저장" 시 필요한 plain text는 항상 **클라이언트가 저장 메시지에 실어서 보낸다**(클라이언트는 이미 Yjs 문서를 갖고 있어 `ytext.toString()`이 자명함).
- 기존 코드 재사용을 최대화한다: `ArticleRepository.findByIdForUpdate`, `ArticleHistoryRecorder.record`, `ArticleCacheStore.putAfterCommit`, `AuthenticationReader.getEditorLabel`을 그대로 호출하는 새 서비스만 추가하고, **`UpdateArticleServiceImpl`은 한 줄도 수정하지 않는다**.
- PUT/이미지 변경이 활성 room에 반영되는 경로는 새 이벤트를 만들지 않고 **기존 `ArticleUpdatedEvent`를 재사용**한다 — 이번 설계에서 가장 중요한 단순화 포인트.
- 새 도메인 패키지 `domain/collaboration/`을 만든다. WS room 생명주기/opaque 릴레이/presence/Redis 라이브 상태는 article·history의 CRUD·diff 관심사와 본질적으로 다르며, 기존에도 "다른 도메인의 `@Service`/`@Component`를 직접 주입"하는 패턴(`UpdateArticleServiceImpl`이 `ArticleHistoryRecorder`를 주입하듯)이 이미 쓰이므로 억지 포트/인터페이스 계층 없이 그대로 따른다.
- 단일 인스턴스 기준 in-memory room 레지스트리로 설계하되, 브로드캐스트 팬아웃을 `CollaborationRoom.broadcast(...)` 한 지점에 모아 향후 Redis Pub/Sub 교체 seam을 남긴다.

## 메시지 프로토콜

JSON 텍스트 프레임 하나로 통일(바이너리 프레임 안 씀 — Jackson이 이미 표준 도구이고 `TextWebSocketHandler` 하나로 코드 경로가 단순해짐, base64 오버헤드는 이 규모에서 무시 가능). 타입 종류가 적고 안정적이므로 폴리모픽 역직렬화 대신 **플랫 DTO + `type` 필드 분기**로 간다.

```kotlin
data class CollaborationMessage(
    val type: String,           // bootstrap | update | awareness | sync | save | saved | external-update | error
    val dataB64: String? = null,   // opaque CRDT 바이트 (update/awareness/sync/bootstrap)
    val content: String? = null,   // save 요청의 plain text / bootstrap·external-update의 plain text
    val imageUrl: String? = null,  // external-update 전용
    val revision: Int? = null,     // saved ack
    val reason: String? = null,    // save의 manual/auto (관측용)
    val message: String? = null,   // error
)
```

| type | 방향 | 의미 |
|---|---|---|
| `bootstrap` | s2c, 접속 직후 1회 | Redis에 상태 있으면 `dataB64`, 없으면 현재 article의 `content`/`imageUrl` |
| `update` | c2s & s2c | Yjs 문서 update(opaque) — 발신자 제외 room 전체 릴레이 |
| `awareness` | c2s & s2c | awareness update(opaque) — 마찬가지로 릴레이만 |
| `sync` | c2s only | 디바운스 주기 전체 스냅샷(`dataB64` + `content`) — Redis 영속화·안전망 저장용, 릴레이 안 함 |
| `save` | c2s only | 명시적 저장(`content` 필수) → `SaveCollaborativeRevisionService` 호출 |
| `saved` | s2c, room 전체 | 저장 완료 ack(`revision`) |
| `external-update` | s2c only | PUT/이미지 업로드로 room 밖에서 바뀐 내용 통지 |
| `error` | s2c only | 파싱 실패 등 |

`sync`는 조율 없이 각 클라이언트가 독립적으로 디바운스 전송, 서버는 조건 없이 덮어쓴다(last-write-wins) — `ArticleCacheStore`가 이미 채택한 "원자성 확보 실익 없음" 철학과 동일. Presence의 stale-peer 정리는 y-protocols/awareness의 내장 타임아웃에 맡기고 서버가 손대지 않는다(내용을 이해해야 하므로 opaque 원칙 위반).

## 구현 상세

### 1. WebSocket 인프라 (`build.mill`, `global/config/CollaborationWebSocketConfig.kt`)
- `build.mill`에 `spring-boot-starter-websocket` 추가.
- 엔드포인트 `/ws/articles/*/collaboration`(named path-var 미지원이라 `session.uri`를 정규식으로 파싱), `WebSocketConfigurer` 구현, `setAllowedOriginPatterns`에 기존 `CorsEnvironment.allowedOrigins` 재사용.
- `ServletServerContainerFactoryBean`으로 메시지 버퍼 크기 상향(기본 8KB → 512KB, 부트스트랩 스냅샷 대비).
- `ArticleCollaborationHandshakeInterceptor`(`HttpSessionHandshakeInterceptor` 상속): `beforeHandshake`에서 세션에 `OAuthSessionAttributes.DATAGSM_USER_INFO` 없으면 401로 컷, articleId 파싱 실패 시 400.
- `AuthenticationReader`에 `getEditorLabel(attributes: Map<String, Any?>)` 오버로드 추가(기존 `HttpSession` 버전과 로직 공유하도록 private 헬퍼로 리팩터).
- 세션은 `ConcurrentWebSocketSessionDecorator`로 감싸 저장(동시 `sendMessage` 호출의 스레드 안전성 확보).

### 2. Room 생명주기 (`domain/collaboration/model/CollaborationRoom.kt`, `service/CollaborationRoomRegistry.kt`)
- `CollaborationRoom`: articleId, 세션 맵, `lastKnownPlainText`, `lastEditorLabel`, 예약된 안전망 `ScheduledFuture`. 모든 조작을 `synchronized(this)`로 room 단위 직렬화(coarse lock으로 충분).
- `CollaborationRoomRegistry`(`@Component`): `ConcurrentHashMap<Long, CollaborationRoom>`, room이 비면 제거.
- 연결 종료 시 room이 비면 기존 `SchedulingConfig.taskScheduler()` 빈으로 디바운스 안전망 저장 예약(재입장 시 취소).

### 3. Redis 영속화 (`global/config/CollaborationRedisConfig.kt`, `CollaborationEnvironment.kt`, `domain/collaboration/service/CollaborationCrdtStateStore.kt`)
- `ArticleCacheRedisConfig`와 동일 패턴의 `RedisTemplate<String, ByteArray>` 빈.
- 키 `collab:crdt-state:{articleId}`, `sync` 메시지 수신 시 `SET ... EX ttlHours`로 갱신. TTL은 정상 정리 경로(퇴장+안전망 저장 후 명시적 삭제)가 실패하는 예외 상황(앱 크래시)에 대한 재해복구용.

### 4. Late-joiner 부트스트랩
접속 직후 1회 `bootstrap` 전송: Redis에 값 있으면 그 바이트, 없으면 기존 `QueryArticleService.execute(articleId)`로 현재 content/imageUrl 전달. (한계: 진행 중 세션에 늦게 들어온 사람이 마지막 flush~현재 사이 변경을 놓칠 이론적 gap 존재 — v1에서는 `sync` 주기를 짧게(3~5초) 잡아 완화하고, peer-assisted bootstrap은 향후 과제로 남김.)

### 5. 저장 → 기존 리비전 파이프라인 (`domain/collaboration/service/SaveCollaborativeRevisionService.kt`)
```kotlin
@Service
class SaveCollaborativeRevisionService(
    private val articleRepository: ArticleRepository,
    private val articleHistoryRecorder: ArticleHistoryRecorder,
    private val articleCacheStore: ArticleCacheStore,
) {
    @Transactional
    fun save(articleId: Long, plainText: String, editor: String): Int? {
        val article = articleRepository.findByIdForUpdate(articleId)
            ?: throw ExpectedException("존재하지 않는 게시글입니다.", HttpStatus.NOT_FOUND)
        val previousContent = article.content
        article.update(plainText, article.imageUrl)
        val revision = articleHistoryRecorder.record(article, previousContent, plainText, editor)
        articleCacheStore.putAfterCommit(ArticleResDto.from(article))
        return revision
    }
}
```
`save` 메시지와 안전망 디바운스 저장 모두 이 서비스를 재사용. `record()`가 diff 없음도 안전하게 처리(리비전 생성 없이 이벤트만 재발행)하므로 안전망이 중복 호출돼도 멱등적으로 무해 — 별도 스킵 최적화 불필요.

### 6. PUT/이미지 ↔ Room 연동 (기존 코드 무수정 핵심)
`UpdateArticleServiceImpl`은 이미 모든 성공적 수정에서 `ArticleUpdatedEvent`를 발행한다(diff 없어도). 이를 재사용하는 새 리스너만 추가:
```kotlin
@Component
class CollaborationExternalUpdateListener(
    private val collaborationRoomRegistry: CollaborationRoomRegistry,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onArticleUpdated(event: ArticleUpdatedEvent) {
        collaborationRoomRegistry.findIfActive(event.articleId)?.broadcast(
            CollaborationMessage(type = "external-update", content = event.content, imageUrl = event.imageUrl),
        )
    }
}
```
Mongo sync 리스너(`@ApplicationModuleListener`)와 **의도적으로 다른 어노테이션**을 쓴다 — 이 브로드캐스트는 "현재 연결된 WS 클라이언트에 대한 best-effort 알림"일 뿐이라 Modulith의 영속화된 재시도/재발행 시맨틱이 적용되면 안 되기 때문(재시작 후 오래된 이벤트를 재생해 "방금 바뀜"이라고 알리면 안 됨).

이미지 실시간 반영(요구사항)도 같은 이벤트로 자동 커버되며, 이미지 전용 흐름만 추가:
- `UpdateArticleImageReqDto`(`image: MultipartFile`), `UpdateArticleImageService`/`Impl` — `UpdateArticleServiceImpl`과 같은 락 순서(R2 업로드 → `findByIdForUpdate` → `article.update(article.content, uploadedUrl)` → `record(article, article.content, article.content, editor)`(diff 없음 브랜치) → `putAfterCommit`).
- `ArticleController`에 `PATCH /api/v1/articles/{articleId}/image` 추가. **기존 PUT은 수정하지 않는다.**

## 신규/수정 파일

**신규**
- `global/config/CollaborationWebSocketConfig.kt`, `CollaborationRedisConfig.kt`, `CollaborationEnvironment.kt`
- `domain/collaboration/websocket/ArticleCollaborationWebSocketHandler.kt`, `ArticleCollaborationHandshakeInterceptor.kt`
- `domain/collaboration/model/CollaborationMessage.kt`, `CollaborationRoom.kt`
- `domain/collaboration/service/CollaborationRoomRegistry.kt`, `CollaborationMessageDispatcher.kt`, `CollaborationCrdtStateStore.kt`, `SaveCollaborativeRevisionService.kt`
- `domain/collaboration/event/CollaborationExternalUpdateListener.kt`
- `domain/article/dto/request/UpdateArticleImageReqDto.kt`, `domain/article/service/UpdateArticleImageService.kt` + `impl/UpdateArticleImageServiceImpl.kt`

**수정**
- `build.mill` (websocket starter 추가)
- `global/security/session/AuthenticationReader.kt` (Map 오버로드)
- `domain/article/controller/ArticleController.kt` (이미지 엔드포인트)
- `src/main/resources/application.yaml` (`collaboration:` 설정)

## 참고한 기존 파일 (시그니처 확인 완료)
- `domain/article/service/impl/UpdateArticleServiceImpl.kt`, `domain/article/repository/ArticleRepository.kt`
- `domain/history/service/ArticleHistoryRecorder.kt`, `domain/history/event/ArticleUpdatedEvent.kt`, `ArticleUpdatedEventListener.kt`
- `domain/article/service/ArticleCacheStore.kt`, `domain/article/service/QueryArticleService.kt`
- `global/security/session/AuthenticationReader.kt`, `global/config/CorsEnvironment.kt`, `global/config/SchedulingConfig.kt`, `global/config/ArticleCacheRedisConfig.kt`

## Phase 순서

1. WS 인프라 + 세션 인증 (handshake, 빈 handler) — 스모크: 핸드셰이크 성공/401 확인
2. Room + 릴레이 + presence (`update`/`awareness`) — 스모크: 실제 Yjs 두 탭 동시 편집
3. Redis 영속화 + late-joiner 부트스트랩 (`sync`/`bootstrap`)
4. 저장 트리거 → 기존 리비전 파이프라인 (`save`/`saved`)
5. PUT/이미지 ↔ room 연동 (`CollaborationExternalUpdateListener`, 이미지 엔드포인트)
6. 안전망 디바운스 저장 (TaskScheduler 연동, room 정리, Redis 키 삭제)
7. 테스트 전체 + 수동 스모크 페이지 정리

## 검증

**자동화 (JUnit5 + Mockito, 기존 컨벤션 그대로 — 실제 소켓/Redis 없음, `mill test`로 CI와 동일하게 실행):**
- `ArticleCollaborationWebSocketHandlerTest`: `WebSocketSession`을 구현한 fake 2개 이상으로 같은 handler에 접속시켜, (a) `update`/`awareness`가 발신자 제외 릴레이되는지, (b) 다른 articleId room 간 격리되는지, (c) `save` → `SaveCollaborativeRevisionService`(mock) 호출 + `saved` 브로드캐스트, (d) 늦은 접속의 `bootstrap`이 Redis 유무에 따라 분기하는지, (e) 마지막 참여자 퇴장 시 주입된 `TaskScheduler`(mock)에 예약이 걸리고 캡처한 runnable을 수동 실행하면 안전망 저장+Redis 삭제+room 제거가 일어나는지, (f) 재접속 시 예약 취소되는지 검증.
- `SaveCollaborativeRevisionServiceTest`(`UpdateArticleServiceImplTest` 패턴), `CollaborationCrdtStateStoreTest`(`ArticleCacheStoreTest` 패턴), `CollaborationRoomRegistryTest`, `CollaborationExternalUpdateListenerTest`, `UpdateArticleImageServiceImplTest`.
- `mill test`, `mill spotless --check`로 CI와 동일하게 확인.

**수동 스모크 (실제 Yjs, 저장소에 커밋하지 않음):**
- Yjs + y-protocols/awareness를 ESM CDN으로 로드하는 최소 HTML을 로컬 스크래치 경로에 두고 두 브라우저 탭에서 로컬 dev 서버(`ws://localhost:8080/ws/articles/{id}/collaboration`)에 연결해 실시간 동시 편집·커서·이미지 브로드캐스트를 육안 확인.

