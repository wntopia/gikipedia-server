# RDB → MongoDB 동기화 이벤트 유실 보완 설계

## 1. 배경 및 목표

현재 `article`/`history` 도메인은 MySQL을 정본(source of truth)으로, MongoDB를 "최신 리비전만 담는 읽기 전용 파생 뷰"로 유지하고 있다. 동기화는 `UpdateArticleServiceImpl` 트랜잭션 커밋 후 `ArticleUpdatedEvent`를 `@TransactionalEventListener(AFTER_COMMIT)`로 처리하는 방식인데, **실패 시 로그만 남기고 무시**하며 재시도·영속화 메커니즘이 없어 이벤트가 유실되면 영구히 Mongo가 stale 상태로 남는다. 이 작업의 목표는 이 유실을 보완하는 것이다.

이번 작업과 함께, 읽기 경로를 MySQL 직접 조회에서 Redis → Mongo 구조로 전환하는 것도 범위에 포함한다. 이를 위해서는 Mongo가 항상 MySQL의 완전한 복제본이어야 하므로, 현재 동기화 이벤트를 전혀 발행하지 않는 문서 생성(create) 흐름도 함께 손본다.

## 2. 범위

### In scope
- MySQL → Mongo 동기화의 유실 방지/재시도/재구동 복구
- 문서 생성(create) 시에도 Mongo 동기화 이벤트를 발행하도록 수정 (현재는 미발행 — 아래 "설계 변경 이력" 참고)
- Redis를 활용한 읽기 캐싱 계층 도입
- 조회 API(`QueryArticleServiceImpl`)의 읽기 경로를 Redis → Mongo로 전환 (feature flag로 토글)
- MySQL ↔ Mongo 간 일일 정합성 검사 및 자동 복구 배치

### Out of scope (명시적으로 제외, 이유 포함)
- **`@Version`(낙관적 락) 도입**: 이건 "동시 편집자 간 lost update 방지"라는 별개 문제를 푸는 기능이다. Mongo 동기화는 이미 revision 비교로 항상 최신 값에 수렴하므로 락 유무와 정합성이 무관하다. 별도 작업으로 분리.
- **문서 삭제(delete) 기능**: 현재 프로젝트에 DELETE API, 삭제 서비스, soft/hard delete 컬럼, `ArticleDeletedEvent` 등이 전혀 없다(전수 조사 확인). 없는 기능을 이번 작업에서 새로 만들지 않는다. 다만 설계는 나중에 delete 이벤트가 추가돼도 같은 골격(Modulith 리스너 + revision 비교)으로 확장 가능하도록 열어둔다.

### 설계 변경 이력 (중요)
초안에는 "읽기 경로 Redis → Mongo → MySQL(최종 폴백)"으로 3단 구조를 검토했으나, 다음 이유로 **폐기하고 Redis → Mongo 2단으로 확정**했다.
- `CreateArticleServiceImpl`이 생성 시 Mongo 동기화 이벤트를 발행하지 않아 "생성 후 한 번도 수정 안 된 문서"가 Mongo에 없는 구조적 갭이 있었음. 이건 위키 서비스 특성상 드문 레이스가 아니라 **영구적으로 지속될 수 있는 흔한 상태**임.
- 이 갭을 읽기 쪽 폴백으로 덮으면, Mongo 동기화가 실제로 장애 나 있어도 항상 MySQL이 정답을 돌려주므로 **장애가 조용히 가려짐(masking)**. 원인(생성 시 미동기화)을 직접 고치는 게 옳음.
- 따라서 §4에 기술한 대로 `CreateArticleServiceImpl`도 생성 직후 동기화 이벤트를 발행하도록 수정하여, Mongo가 항상 MySQL의 완전한 복제본이 되게 한다. 이후 Mongo miss는 "정상 케이스"가 아니라 "진짜 동기화 장애"를 뜻하므로, 폴백으로 덮지 않고 그대로 드러나야 한다(404).

## 3. 아키텍처 개요

```
[쓰기 — 생성]
  ArticleController.create()
    └─ CreateArticleServiceImpl (@Transactional)
         ├─ articles insert
         └─ ApplicationEventPublisher.publishEvent(ArticleUpdatedEvent(articleId, revision = 1))
       ── 커밋 ──
    └─ 응답: 생성된 entity 그대로 반환 (RYOW)
    └─ Redis SET article:cache:{id} = {revision: 1, title, content}, EX 1h  ← 쓰기 요청 스레드가 직접, 동기 실행
  (article_histories/snapshots는 기존 규칙대로 건드리지 않음 — 첫 "수정" 시점에 소급 생성되는 규칙 유지.
   MySQL의 리비전 체계는 "revision 1 = 생성 시점 그대로(0번 수정)"이므로, Mongo도 생성 시 revision=1로 맞춰
   번호 체계를 통일한다. 이후 최초 수정이 발행하는 이벤트는 revision=2이며 자연스럽게 이어받는다)

[쓰기 — 수정]
  ArticleController.update()
    └─ UpdateArticleServiceImpl (@Transactional)
         ├─ articles 갱신 (dirty checking)
         ├─ ArticleHistoryRecorder.record() → article_histories/article_snapshots insert
         └─ ApplicationEventPublisher.publishEvent(ArticleUpdatedEvent(articleId, revision))
       ── 커밋 ──
    └─ 응답: 방금 갱신한 entity 그대로 반환 (RYOW, 기존 방식 유지)
    └─ Redis SET article:cache:{id} = {revision, title, content}, EX 1h  ← 쓰기 요청 스레드가 직접, 동기 실행

  (생성/수정 커밋과 동시에, Spring Modulith가 event_publication 테이블에 발행 기록 생성 — 하나의 리스너가 둘 다 처리)

[동기화 — Spring Modulith Event Publication Registry]
  @ApplicationModuleListener  (기존 @TransactionalEventListener(AFTER_COMMIT) 대체)
  fun on(event: ArticleUpdatedEvent) = articleMongoSyncService.sync(event.articleId, event.revision)

  ArticleMongoSyncService.sync(articleId, revision) — 2단계 원자적 반영, 두 스레드가 동시에 같은
  article을 처리해도 앱 레벨 락 없이 항상 더 높은 revision이 최종적으로 남는다:
    1) findAndModify(filter: {document_id: id}, update: {$max: {revision}}, upsert: true, returnNew: true)
       → revision 필드만 원자적으로 "선점". 반환된 revision이 자신이 보낸 값과 다르면(=이미 더 높은 값이
         반영돼있음) 여기서 멈추고 title/content는 건드리지 않는다(stale 이벤트 무시).
    2) 선점에 성공했을 때만 MySQL을 재조회해 title/content 등을 얻고,
       updateFirst(filter: {document_id: id, revision: 그 값 그대로}, update: {$set: title/content/...})
       → 그사이 더 최신 이벤트가 revision을 앞질렀다면 필터가 안 맞아 조용히 스킵되므로, 오래된 내용으로
         되돌아가는 일이 없다.
  - 성공: event_publication 행 즉시 삭제
  - 실패: 행이 미완료 상태로 남음
  - 앱 재시작 시: republish-outstanding-events-on-restart=true 로 자동 드레인
  - 10분마다 @Scheduled: registry.findIncompletePublications() 재시도 (고정 주기, 지수 백오프 없음)

[읽기]
  QueryArticleServiceImpl (feature flag: article.read.source=redis-mongo | mysql)
    1. Redis GET article:cache:{id} → hit면 반환
    2. miss → Mongo findByDocumentId(id) → hit면 반환 + Redis backfill(SET, EX 1h)
    3. 전부 miss → 404
       (생성 이벤트가 이미 Mongo에 revision=1 baseline을 만들어뒀으므로, 여기서 miss가 뜨는 건
        "정상 케이스"가 아니라 동기화가 실제로 지연/실패했다는 신호. MySQL로 조용히 폴백해 가리지 않고
        그대로 404를 반환한다 — 이래야 장애가 정합성 배치/로그로 드러나고 방치되지 않음)

[보험 — 일일 정합성 배치]
  매일 04:00 @Scheduled
  - articles 전체 대상으로, article_histories의 article_id별 MAX(revision)(없으면 1 — 아직 한 번도
    수정 안 된 문서는 "생성 시점 그대로"이므로 리비전 1이 정답) 조회
  - Mongo의 documentId별 revision과 비교
  - 불일치 발견 시 자동 복구: 동기화 리스너와 동일한 upsert 로직 재사용하여 즉시 맞춤
```

## 4. 핵심 설계 결정과 근거

| 결정 | 선택 | 근거 |
|---|---|---|
| 동기화 메커니즘 | 커스텀 cursor 폴링 워커 대신 **Spring Modulith Event Publication Registry** 활용 | 이미 있는 의존성(`spring-modulith-starter-jpa`)으로 영속화+재시도+재시작 드레인을 대부분 해결. ID 커서가 아니라 완료 플래그 기반이라 "커밋 순서 어긋남 방어용 2초 지연" 자체가 불필요해짐 |
| 이벤트 payload | `{articleId, revision}`만 (title/content 제외) | 리스너가 MySQL을 재조회해 최신값을 쓰므로, 이벤트 DTO가 스키마 진화에 거의 영향받지 않음 |
| 역직렬화 안정성 | ObjectMapper `FAIL_ON_UNKNOWN_PROPERTIES=false` | 이벤트 필드가 늘어나도 과거 저장된 payload 역직렬화가 깨지지 않음 |
| event_publication 완료 행 | 완료 즉시 삭제 (`completion-mode=DELETE`) | article_histories가 이미 감사/이력 용도이므로, 이 테이블은 순수 대기열 용도로만 사용, 무한정 커지지 않게 |
| 백업 재시도 주기 | 고정 10분 간격 (`@Scheduled` + `findIncompletePublications()`) | 실패가 드문 트래픽 규모에서 지수 백오프는 과설계. 단순 고정 주기로 충분 |
| Redis 쓰기 | 조건부 CAS(Lua) 없이 **단순 SET + TTL(1시간)** | Redis는 최말단 캐시(정본 MySQL, 복제본 Mongo 다음 계층)라 레이스가 나도 TTL/backfill로 자연 치유됨. 원자성 확보 비용(Lua) 대비 실익이 작음 |
| Redis TTL 값 | 1시간, 매 쓰기마다 리셋 | 매 수정 시 능동 SET이 이루어지므로 정상 상황에선 TTL이 거의 의미 없음. 오직 "능동 갱신이 실패한 드문 경우"의 보험이라 짧게 잡을 이유가 없고, 오히려 짧으면 캐시 히트율만 깎임 |
| Redis SET 실행 위치 | 쓰기 요청 스레드에서 MySQL 커밋 직후 동기 실행 | 다른 사용자도 거의 즉시 최신값을 캐시에서 볼 수 있음 (지연은 Redis 호출 1회 수준) |
| Mongo 쓰기 방식 | 앱 코드의 "조회→비교→delete+save" 대신 **2단계 원자적 반영** — 1) `$max`로 revision 필드만 먼저 원자적으로 선점 2) 선점 성공 시에만 MySQL 재조회 후 `{document_id, revision}` 필터로 `$set` (그사이 더 최신 이벤트가 앞질렀으면 필터가 안 맞아 조용히 스킵) | `$lt` 필터 하나짜리 upsert는 파이프라인 업데이트 없이는 "문서 없음"과 "이미 최신임" 두 케이스를 동시에 올바르게 못 만족시킴(uniq 인덱스 위반 위험). `$max` 2단계는 표준 업데이트 연산자만으로 완전한 원자성을 달성 |
| 낙관적 락(@Version) | 이번 작업 범위에서 완전 제외 | Mongo 동기화 정합성과 무관한 별개 문제(동시 편집자 lost update 방지)이므로 |
| 삭제 처리 | 이번 작업 범위에서 완전 제외 | 삭제 기능 자체가 프로젝트에 없음. 설계는 나중에 확장 가능하도록만 열어둠 |
| 생성(create) 시 동기화 | `CreateArticleServiceImpl`도 저장 직후 `ArticleUpdatedEvent(revision=1)` 발행하도록 수정 | 현재 create는 이벤트를 전혀 안 쏴서 "생성만 되고 미수정"인 문서가 Mongo에 영구히 없는 구조적 갭이 있었음(위키 특성상 흔한 상태). 읽기 쪽 폴백으로 덮기보다 원인(생성 시 미동기화)을 직접 막는 게 근본적. revision=1은 MySQL의 "리비전 1 = 생성 시점 그대로" 체계와 통일한 값(0 같은 특수값 도입 안 함) |
| 읽기 경로 전환 | 이번 작업에 포함, **Redis→Mongo 2단 (MySQL 폴백 없음)** | create도 동기화 대상이 되면 Mongo는 항상 MySQL의 완전한 복제본이어야 함. 여기서 MySQL 폴백을 두면 Mongo 동기화가 실제로 고장나도 항상 정답이 나와 장애가 조용히 가려짐(masking). 폴백 없이 miss를 그대로 드러내야 문제를 놓치지 않음 |
| 정합성 배치 스캔 범위 | 전체 article 스캔 (수천~수만 건 규모 가정) | 이 규모에서 단순 전체 스캔의 부하가 무시할 만한 수준. "최근 변경분만" 최적화는 불필요 |
| 정합성 배치 동작 | 자동 복구 (불일치 발견 즉시 MySQL 기준으로 Mongo 맞춤) | 사람 개입 없이 수렴 |
| 정합성 배치 실행 시각 | 매일 새벽 4시 | 트래픽 최소 시간대로 DB 부하 최소화 |
| Feature flag | `application.yaml` 불리언 값, 재시작 허용 | 단일 서버·중소 규모라 런타임 무중단 토글 인프라(DB 기반 설정 등) 도입은 과함 |

## 5. 구현 체크리스트 (파일 단위)

1. **의존성/설정**
   - `application.yaml`: `spring.modulith.events.republish-outstanding-events-on-restart=true`, completion-mode 설정, `article.read.source` 플래그, Redis 관련 TTL 설정값
   - `@EnableScheduling` 추가 (현재 프로젝트에 스케줄링 인프라 없음)

2. **이벤트/리스너**
   - `ArticleUpdatedEvent.kt`: `title`, `content` 필드 제거, `articleId`, `revision`만 유지
   - `ArticleUpdatedEventListener.kt`: `@TransactionalEventListener(AFTER_COMMIT)` → `@ApplicationModuleListener`로 교체, 실제 동기화는 신규 `ArticleMongoSyncService`에 위임
   - 신규 `ArticleMongoSyncService.kt`: MySQL 재조회 + Mongo 2단계 원자적 반영(`$max` 선점 → 조건부 `$set`) 로직. 리스너와 일일 정합성 배치가 공유
   - `ArticleHistoryRecorder.kt`: 이벤트 발행 시 payload 변경 반영
   - `CreateArticleServiceImpl.kt`: `articleRepository.save()` 직후 `ApplicationEventPublisher.publishEvent(ArticleUpdatedEvent(articleId, revision = 1))` 추가 (article_histories/ArticleHistoryRecorder는 건드리지 않음 — Mongo 동기화 목적의 이벤트만 추가)

3. **백업 재시도 스케줄러**
   - 신규: 미완료 event_publication 10분 주기 재시도 (`EventPublicationRegistry.findIncompletePublications()` 활용)

4. **Redis 캐싱**
   - `UpdateArticleServiceImpl.kt`, `CreateArticleServiceImpl.kt`: 커밋 후 응답 구성 직후 Redis SET 로직 추가
   - `RedisTemplate`/`RedisConfig` 신규 (현재 세션 용도 외 애플리케이션 캐싱 Redis 설정 없음)

5. **읽기 경로**
   - `QueryArticleServiceImpl.kt`: feature flag 분기, Redis→Mongo 순차 조회 + backfill 로직. 둘 다 miss면 그대로 404 (MySQL 폴백 없음)

6. **일일 정합성 배치**
   - 신규 스케줄러: article_histories MAX(revision)(없으면 1 — 미수정 문서는 생성 시점 그대로) vs Mongo revision 비교, 불일치 시 `ArticleMongoSyncService.sync()` 재사용해 자동 복구

## 6. 남은 구현 시 유의사항

- `ArticleMongoEntity`에 `document_id` 인덱스가 없음 (기존 이슈) — 조회 경로가 Mongo를 직접 타게 되므로 이번 작업에서 `@Indexed(unique = true)` 추가.
- create 이벤트(`revision = 1`)와 첫 수정 이벤트(`revision = 2`) 이후 계속 이어지는 단일 번호 체계이므로, `ArticleMongoSyncService`가 이 두 케이스 모두에서 올바르게 동작하는지(특히 최초 생성 시 upsert insert 경로) 테스트 케이스로 반드시 확인.
- Mongo가 miss인데 MySQL엔 article이 존재하는 상황이 실제로 관측되면, 이는 버그가 아니라 "동기화가 지연/실패했다"는 신호이므로 404를 그대로 반환하고 넘어가지 말고 원인(리스너 실패 로그, event_publication 미완료 행)을 확인해야 한다.
