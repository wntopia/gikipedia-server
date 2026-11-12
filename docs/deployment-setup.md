# 배포 서버 준비 명령어 모음

CD 파이프라인(`cd-prod.yml`, `cd-stage.yml`)을 쓰기 전에 로컬/원격에서 실행해야 하는 명령어들이다.
prod/stage 둘 다 같은 절차를 반복하면 된다. 아래에서는 `ubuntu` 계정, prod 기준으로 적었다.

## 1. 로컬에서 SSH 키 페어 생성

```
ssh-keygen -t ed25519 -C "gikipedia-cd" -f ./gikipedia_deploy_key -N ""
```

stage용은 파일명을 다르게 해서 따로 만든다.

```
ssh-keygen -t ed25519 -C "gikipedia-cd-stage" -f ./gikipedia_deploy_key_stage -N ""
```

## 2. 공개키를 원격 서버에 등록

포트/호스트는 실제 값으로 바꿔서 사용한다.

```
ssh-copy-id -i ./gikipedia_deploy_key.pub -p 24143 ubuntu@ssh.gsmsv.site
```

`ssh-copy-id`가 비밀번호 인증 거부로 실패하면, 이미 접속 가능한 계정으로 들어가서 수동으로 추가한다.

```
cat ./gikipedia_deploy_key.pub | ssh -p 24143 ubuntu@ssh.gsmsv.site "mkdir -p ~/.ssh && chmod 700 ~/.ssh && cat >> ~/.ssh/authorized_keys && chmod 600 ~/.ssh/authorized_keys"
```

## 3. 개인키 내용을 GitHub Secret에 등록

아래 명령으로 개인키 전문을 출력해서 그대로 복사한다.

```
cat ./gikipedia_deploy_key
```

`PROD_SSH_KEY` (stage는 `STAGE_SSH_KEY`) Secret에 붙여넣는다.

## 4. 원격 서버 접속 확인

```
ssh -i ./gikipedia_deploy_key -p 24143 ubuntu@ssh.gsmsv.site
```

## 5. (원격 서버에서 실행) sudo 비밀번호 없이 systemctl 허용

```
sudo visudo -f /etc/sudoers.d/gikipedia-deploy
```

편집기가 열리면 아래 두 줄을 넣고 저장한다.

```
ubuntu ALL=(root) NOPASSWD: /usr/bin/systemctl restart gikipedia-server, /usr/bin/systemctl status gikipedia-server
ubuntu ALL=(root) NOPASSWD: /usr/bin/systemctl restart gikipedia-server-stage, /usr/bin/systemctl status gikipedia-server-stage
```

`systemctl` 경로가 다를 수 있으니 먼저 확인 후 위 값을 맞춘다.

```
which systemctl
```

## 6. (원격 서버에서 실행) Java 25 설치

```
sudo apt update
sudo apt install -y wget apt-transport-https gnupg
wget -O - https://packages.adoptium.net/artifactory/api/gpg/key/public | sudo tee /etc/apt/trusted.gpg.d/adoptium.asc
echo "deb https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | sudo tee /etc/apt/sources.list.d/adoptium.list
sudo apt update
sudo apt install -y temurin-25-jdk
java -version
```

## 6-1. (원격 서버에서 실행) Docker 설치 — SeaweedFS 구동용

CD 파이프라인이 배포할 때마다 `docker compose up -d`로 SeaweedFS(master/volume/filer)를 띄운다.
Docker가 없으면 이 단계에서 배포가 실패하므로 미리 설치해 둔다.

```
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker ubuntu
```

`usermod` 반영을 위해 SSH 세션을 재접속하거나 `newgrp docker`로 그룹을 다시 적용한 뒤 확인한다.

```
docker compose version
```

## 7. (원격 서버에서 실행) systemd 서비스 파일 생성 — production

```
sudo tee /etc/systemd/system/gikipedia-server.service > /dev/null <<'EOF'
[Unit]
Description=Gikipedia Server (Production)
After=network.target

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/home/ubuntu/gikipedia-server
ExecStart=/bin/sh /home/ubuntu/gikipedia-server/out/launcher.dest/run
Restart=on-failure
RestartSec=5
SuccessExitStatus=143
Environment=SPRING_PROFILES_ACTIVE=prod

[Install]
WantedBy=multi-user.target
EOF
```

## 8. (원격 서버에서 실행) systemd 서비스 파일 생성 — stage

```
sudo tee /etc/systemd/system/gikipedia-server-stage.service > /dev/null <<'EOF'
[Unit]
Description=Gikipedia Server (Stage)
After=network.target

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/home/ubuntu/gikipedia-server-stage
ExecStart=/bin/sh /home/ubuntu/gikipedia-server-stage/out/launcher.dest/run
Restart=on-failure
RestartSec=5
SuccessExitStatus=143
Environment=SPRING_PROFILES_ACTIVE=stage

[Install]
WantedBy=multi-user.target
EOF
```

## 9. (원격 서버에서 실행) systemd 서비스 등록 및 활성화

```
sudo systemctl daemon-reload
sudo systemctl enable gikipedia-server
sudo systemctl enable gikipedia-server-stage
```

## 10. mill 실행 스크립트 확인

`./mill assembly`로 만든 fat jar는 Spring Boot 모듈별 `META-INF/spring/*.imports`·`spring.factories`가
하나만 남고 덮어써져 auto-configuration이 동작하지 않는다(JPA Repository·Tomcat 등이 등록되지 않아 기동 실패).
그래서 의존성 jar를 classpath로 그대로 넘기는 `./mill launcher` 실행 스크립트를 사용한다.
스크립트에 shebang이 없어 systemd가 직접 exec하면 `Exec format error`가 나므로 `ExecStart`에서 `/bin/sh`로 실행한다.
스크립트는 PATH의 `java`(Java 25)로 실행되고, 서버의 coursier 캐시와 `out/compile.dest`를 참조하므로
배포 디렉터리에서 `./mill clean`을 하면 다음 배포 전까지 서비스가 뜨지 않는다.

```
./mill launcher
ls out/launcher.dest/run
```

## 11. GitHub Secrets 등록 (gh CLI 사용 시)

```
gh secret set PROD_SSH_HOST
gh secret set PROD_SSH_USERNAME
gh secret set PROD_SSH_KEY < ./gikipedia_deploy_key
gh secret set PROD_SSH_PORT
gh secret set PROD_APP_DIR
gh secret set PROD_APPLICATION_YAML < ./application.yaml
```

stage도 동일하게 `STAGE_` 접두사로 반복한다.

```
gh secret set STAGE_SSH_HOST
gh secret set STAGE_SSH_USERNAME
gh secret set STAGE_SSH_KEY < ./gikipedia_deploy_key_stage
gh secret set STAGE_SSH_PORT
gh secret set STAGE_APP_DIR
gh secret set STAGE_APPLICATION_YAML < ./application-stage.yaml
```
