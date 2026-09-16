# 배포(CD) 환경변수 가이드

`.github/workflows/cd-prod.yml`, `.github/workflows/cd-stage.yml`이 사용하는 GitHub Actions Secrets 목록이다.
Repo → Settings → Secrets and variables → Actions → New repository secret에서 등록한다.

## SSH 접속 정보

| Secret | 설명 | 예시 |
|---|---|---|
| `PROD_SSH_HOST` | 운영 서버 호스트 | `ssh.gsmsv.site` |
| `PROD_SSH_USERNAME` | SSH 접속 계정 | `ubuntu` |
| `PROD_SSH_KEY` | SSH 접속용 개인키 전문 (패스프레이즈 없는 키) | `-----BEGIN OPENSSH PRIVATE KEY-----...` |
| `PROD_SSH_PORT` | SSH 포트 | `24143` |
| `STAGE_SSH_HOST` | 스테이지 서버 호스트 | |
| `STAGE_SSH_USERNAME` | 스테이지 SSH 접속 계정 | `ubuntu` |
| `STAGE_SSH_KEY` | 스테이지 SSH 개인키 전문 | |
| `STAGE_SSH_PORT` | 스테이지 SSH 포트 | |

## 배포 경로

| Secret | 설명 | 예시 |
|---|---|---|
| `PROD_APP_DIR` | 운영 서버에서 저장소를 clone/pull할 절대경로 | `/home/ubuntu/gikipedia-server` |
| `STAGE_APP_DIR` | 스테이지 서버 저장소 경로 | `/home/ubuntu/gikipedia-server-stage` |

## application.yaml 전문

`src/main/resources/application.yaml`은 `.gitignore`에 등록되어 있고, CD 파이프라인이 배포 시점에
아래 Secret 전문으로 이 경로를 덮어쓴다. 로컬 템플릿은 `application.yaml.example` 참고.

| Secret | 설명 |
|---|---|
| `PROD_APPLICATION_YAML` | 운영용 `application.yaml` 전체 내용 (실제 값 채운 버전) |
| `STAGE_APPLICATION_YAML` | 스테이지용 `application.yaml` 전체 내용 |

### `application.yaml` 안에 채워야 할 값 (환경변수로 참조되는 항목)

`application.yaml.example`에서 `${VAR:default}` 형태로 선언된 항목들이다.
Secret에 넣는 `application.yaml` 전문에는 아래 값들을 실제 값으로 바로 채워 넣거나,
서버 환경변수로 주입한다.

| 환경변수 | 용도 |
|---|---|
| `DATAGSM_OAUTH_CLIENT_ID` | DataGSM OAuth 클라이언트 ID |
| `DATAGSM_OAUTH_CLIENT_SECRET` | DataGSM OAuth 클라이언트 시크릿 |
| `DATAGSM_OAUTH_REDIRECT_URI` | OAuth 콜백 URL (운영 도메인 기준, `localhost` 아님) |
| `DATAGSM_OAUTH_SCOPE` | OAuth 스코프 |
| `DATAGSM_OAUTH_SUCCESS_REDIRECT_URI` | 로그인 성공 리다이렉트 경로 |
| `DATAGSM_OAUTH_FAILURE_REDIRECT_URI` | 로그인 실패 리다이렉트 경로 |
| `DATAGSM_OAUTH_AUTHORIZATION_URI` | DataGSM OAuth authorization 엔드포인트 |
| `DATAGSM_OAUTH_TOKEN_URI` | DataGSM OAuth token 엔드포인트 |
| `DATAGSM_OAUTH_USER_INFO_URI` | DataGSM OAuth user-info 엔드포인트 |
| `SEAWEEDFS_FILER_URL` | SeaweedFS Filer 엔드포인트. 앱은 systemd로 호스트에서 직접 뜨고 SeaweedFS는 `docker-compose.yml`로 같은 호스트에 포트가 바인딩되므로 `http://localhost:8888`을 쓴다(컨테이너 서비스명이 아님). |
| `SEAWEEDFS_PUBLIC_URL` | SeaweedFS 퍼블릭 URL (이미지 조회에 쓰이는 base URL). 클라이언트(브라우저)가 직접 접근하므로 서버가 외부에서 접근 가능한 도메인/IP여야 한다 — `localhost`는 운영에서 쓸 수 없다. |
| `ARTICLE_REVISION_CACHE_MAX_ENTRIES` | 아티클 리비전 캐시 최대 엔트리 수 (기본값 있음) |
| `ARTICLE_REVISION_CACHE_TTL_HOURS` | 아티클 리비전 캐시 TTL (기본값 있음) |
| `COLLABORATION_CRDT_TTL_HOURS` | 협업 CRDT TTL (기본값 있음) |
| `COLLABORATION_LEAVE_DEBOUNCE_SECONDS` | 협업 퇴장 디바운스 시간 (기본값 있음) |
| `ARTICLE_SNAPSHOT_INTERVAL` | 아티클 스냅샷 간격 (기본값 있음) |
| `ARTICLE_HISTORY_COMPACTION_ENABLED` | 히스토리 압축 배치 활성화 여부 (기본값 있음) |
| `ARTICLE_READ_SOURCE` | 아티클 조회 소스 (`redis-mongo` / `mysql`, 기본값 있음) |
| `ARTICLE_CACHE_TTL_MINUTES` | 아티클 캐시 TTL (기본값 있음) |

> **참고:** MongoDB / Redis / MySQL 연결 정보(`spring.data.mongodb.uri`,
> `spring.data.redis.host`/`port`, `spring.datasource.url`/`username`/`password` 등)는
> 현재 `application.yaml.example`에 플레이스홀더로 선언되어 있지 않다.
> 운영에서 실제 DB에 연결하려면 `PROD_APPLICATION_YAML` / `STAGE_APPLICATION_YAML` 전문에
> 해당 접속 정보를 직접 추가해야 한다.

## Discord 알림 (공통, 기존 CI와 동일 Secret 재사용)

| Secret | 설명 |
|---|---|
| `DISCORD_INFORMATION_ALERT_CHANNEL_WEBHOOK` | 배포 결과 알림용 Discord 웹훅 URL |
