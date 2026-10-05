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

> **참고:** MongoDB / Redis / MySQL 연결 정보(`spring.mongodb.uri`,
> `spring.data.redis.host`/`port`, `spring.datasource.url`/`username`/`password` 등)는
> 현재 `application.yaml.example`에 플레이스홀더로 선언되어 있지 않다.
> 운영에서 실제 DB에 연결하려면 `PROD_APPLICATION_YAML` / `STAGE_APPLICATION_YAML` 전문에
> 해당 접속 정보를 직접 추가해야 한다.

## DB 비밀번호 (docker compose)

MySQL·MongoDB·Redis는 `docker-compose.yml`로 앱과 같은 호스트에 뜨며, 포트는 `127.0.0.1`에만 바인딩된다.
비밀번호는 기본값이 없고, CD가 아래 Secret으로 배포 디렉터리에 `.env`(권한 600, gitignore 대상)를 만들어 주입한다.
값에 작은따옴표(`'`)는 쓰지 않는다. 계정명·DB명은 기본값 `gikipedia`를 쓴다.

| Secret | 설명 |
|---|---|
| `STAGE_MYSQL_PASSWORD` / `PROD_MYSQL_PASSWORD` | MySQL `gikipedia` 계정 비밀번호 |
| `STAGE_MYSQL_ROOT_PASSWORD` / `PROD_MYSQL_ROOT_PASSWORD` | MySQL root 비밀번호 |
| `STAGE_MONGO_PASSWORD` / `PROD_MONGO_PASSWORD` | MongoDB root(`gikipedia`) 비밀번호 |
| `STAGE_REDIS_PASSWORD` / `PROD_REDIS_PASSWORD` | Redis `requirepass` |

`*_APPLICATION_YAML`의 접속 정보도 같은 값이어야 한다.

- `spring.datasource.username`: `gikipedia`
- `spring.datasource.password`: `*_MYSQL_PASSWORD`
- `spring.mongodb.uri`: `"mongodb://gikipedia:<*_MONGO_PASSWORD>@localhost:27017/gikipedia?authSource=admin"`
  - Spring Boot 4부터 `spring.data.mongodb.uri`가 아니라 `spring.mongodb.uri`이다(`auto-index-creation`은 `spring.data.mongodb` 그대로).
  - 따옴표로 감싸고, 비밀번호의 `@ : / % ? #`는 URL 인코딩한다.
- `spring.data.redis.password`: `*_REDIS_PASSWORD`

yaml에 아래 값도 반드시 있어야 앱이 기동된다.

- `spring.jpa.hibernate.ddl-auto: update`: 마이그레이션 도구가 없으므로 빈 DB에 테이블(`event_publication` 등)을 만든다.
- `spring.security.oauth2.client.registration.datagsm.client-id` / `client-secret`: 비어 있으면 DataGSM SDK가 기동을 거부한다.
- `seaweedfs.filer-url` / `seaweedfs.public-url`: 최상위 `seaweedfs` 키다(`spring.cloud.*` 아님).

> MySQL·MongoDB의 비밀번호는 볼륨이 처음 생성될 때만 적용된다. 이후 Secret을 바꾸려면 DB 안에서 비밀번호를 직접 변경하거나 볼륨을 지우고 다시 만들어야 한다.

## 기동 확인 (Repository Variables)

배포 스크립트는 `systemctl restart` 후 앱 포트가 HTTP 응답을 줄 때까지 최대 180초 기다린다.
그 사이 프로세스가 종료되거나 재시작되면, 또는 시간 안에 응답이 없으면 배포를 실패로 처리한다.
Secret이 아니라 Settings → Secrets and variables → Actions → **Variables** 탭에 등록한다.

| Variable | 설명 | 기본값 |
|---|---|---|
| `STAGE_APP_PORT` | 스테이지 앱이 listen하는 포트 (`server.port`) | `8080` |

## Discord 알림 (공통, 기존 CI와 동일 Secret 재사용)

| Secret | 설명 |
|---|---|
| `DISCORD_INFORMATION_ALERT_CHANNEL_WEBHOOK` | 배포 결과 알림용 Discord 웹훅 URL |
