# MCJ 운영 모니터링 반영 가이드

## 1. 개요
이 문서는 `Prometheus + Grafana + Alertmanager` 모니터링 스택을 운영 서버(prod)에 반영하는 절차를 정리합니다.

핵심 원칙:
- 앱 런타임 환경변수: 루트 `.env.prod`
- 모니터링/프록시 환경변수: `docker/.env`
- 디스코드 알림은 `critical`/`warning` 웹훅 분리

## 2. 사전 조건
- DNS 레코드
  - `api.moongchijang.com -> 운영 서버 IP`
  - `grafana.moongchijang.com -> 운영 서버 IP`
- 보안 그룹/방화벽
  - `80`, `443` 인바운드 허용
- 서버에 Docker / Docker Compose 사용 가능

## 3. 환경변수 설정

### 3.1 앱(.env.prod)
루트 경로의 `.env.prod`는 스프링 앱(`app-prod`)용입니다.
`DB`, `REDIS`, `JWT`, `외부 API 키` 등 앱 관련 값만 관리합니다.

### 3.2 모니터링/프록시(docker/.env)
`docker/.env`는 `docker/docker-compose.yml`에서 사용하는 값입니다.

예시:

```env
ECR_REPOSITORY_URL=<ECR URI>

GRAFANA_ADMIN_USER=mcj_grafana_admin
GRAFANA_ADMIN_PASSWORD=<STRONG_PASSWORD>
GRAFANA_SERVER_DOMAIN=grafana.moongchijang.com
GRAFANA_SERVER_ROOT_URL=https://grafana.moongchijang.com

ALERT_ENV=prod
ALERT_SERVICE=moongchijang-be
DISCORD_WEBHOOK_MONITORING_CRITICAL=<DISCORD_WEBHOOK_URL>
DISCORD_WEBHOOK_MONITORING_WARNING=<DISCORD_WEBHOOK_URL>
```

생성 방법:

```bash
cd docker
cp .env.example .env
vi .env
chmod 600 .env
```

## 4. SSL 인증서 발급
`docker` 디렉터리에서 실행합니다.

```bash
docker run --rm \
  -v "$(pwd)/certbot/conf:/etc/letsencrypt" \
  -v "$(pwd)/certbot/www:/var/www/certbot" \
  certbot/certbot certonly --webroot \
  -w /var/www/certbot \
  -d api.moongchijang.com \
  -d grafana.moongchijang.com \
  --email <YOUR_EMAIL> --agree-tos --no-eff-email \
  --expand --non-interactive
```

## 5. 스택 기동
`docker` 디렉터리에서 실행합니다.

```bash
docker compose --profile prod --profile monitoring up -d \
  nginx app-prod prometheus alertmanager grafana
```

## 6. 점검 체크리스트

### 6.1 서비스 접근
- `https://api.moongchijang.com` 응답 정상
- `https://grafana.moongchijang.com` 접속 가능
- Grafana 관리자 계정 로그인 가능

### 6.2 Prometheus 타깃
- Prometheus UI에서 `app-prod:8081` 타깃 상태 `UP`
- dev 환경을 함께 운영하는 경우 `app-dev:8081` 타깃 상태 `UP` 확인
- `job=app` 메트릭 수집 확인

### 6.3 Alertmanager 라우팅
- `warning` 알림은 warning 웹훅 채널로 전송
- `critical` 알림은 critical 웹훅 채널로 전송
- 알림 제목 prefix 확인: `[prod][warning]`, `[prod][critical]`

### 6.4 대시보드 확인
- `MCJ Prod Monitoring Overview` 대시보드 노출
- dev 환경을 함께 운영하는 경우 `MCJ Dev Load Test Monitoring` 대시보드 노출
- 아래 패널 데이터 확인
  - 가용성, RPS, 5xx, p95/p99, CPU, Heap, GC, Disk, Auth 이벤트

## 7. 트러블슈팅

### 7.1 `no such service: grafana`
- 서버 코드가 최신이 아닐 수 있음
- 최신 브랜치 pull 후 `docker compose config --services` 확인

### 7.2 인증서 발급 중 `EOFError`
- 인터랙티브 선택 프롬프트 문제
- `--expand --non-interactive` 옵션으로 재실행

### 7.3 Grafana 502/연결 실패
- `docker compose ps`에서 grafana 컨테이너 상태 확인
- `docker logs moongchijang-nginx --tail 100`
- `docker logs moongchijang-grafana --tail 100`

## 8. 운영 권장 사항
- 디스코드 채널 분리
  - `mcj-prod-alert-critical`
  - `mcj-prod-alert-warning`
- `docker/.env`는 Git 커밋 금지
- 인증서 갱신 자동화(cron) 설정 권장
