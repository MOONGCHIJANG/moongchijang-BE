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
- `MCJ Prod Payment Monitoring` 대시보드 노출
- dev 환경을 함께 운영하는 경우 `MCJ Dev Load Test Monitoring` 대시보드 노출
- dev 환경을 함께 운영하는 경우 `MCJ Dev Payment Monitoring` 대시보드 노출
- 아래 패널 데이터 확인
  - 가용성, RPS, 5xx, p95/p99, CPU, Heap, GC, Disk, Auth 이벤트
  - 결제 시도/성공/실패, 결제 성공률, 결제 완료 API 지연, 웹훅 실패율, 환불 처리 지표
  - 결제 주문 생성/승인/취소/웹훅/환불 도메인 metric, PortOne API 성공/실패 및 p95 latency
  - 결제 도메인 metric은 환경별 `job` 라벨(`app`, `app-dev`) 기준 분리 확인

### 6.5 결제 모니터링 부하테스트
- 기본 시나리오는 존재하지 않는 결제 주문의 완료 요청을 발생시켜, 운영 데이터 변경 없이 결제 실패/HTTP 지표가 Grafana에 노출되는지 확인한다.
- 실제 결제 주문 생성 부하는 `RUN_CREATE_ORDER=true`를 명시한 경우에만 실행한다.
- dev 환경 실행 절차와 결과 기록 기준은 `docs/dev-load-test-monitoring-guide.md`를 우선 참고한다.

```bash
k6 run \
  -e MCJ_BASE_URL=https://api.moongchijang.com \
  -e MCJ_ACCESS_TOKEN=<buyer-access-token> \
  -e MCJ_SCENARIO_NAME=payment-monitoring \
  -e VUS=3 \
  -e DURATION=1m \
  load-tests/scenarios/payment-monitoring.js
```

- 결제 주문 생성 지표까지 확인할 때만 아래 옵션을 추가한다.

```bash
k6 run \
  -e MCJ_BASE_URL=https://api.moongchijang.com \
  -e MCJ_ACCESS_TOKEN=<buyer-access-token> \
  -e MCJ_SCENARIO_NAME=payment-monitoring \
  -e MCJ_GROUP_BUY_ID=<active-group-buy-id> \
  -e ALLOW_STATE_CHANGE=true \
  -e RUN_CREATE_ORDER=true \
  -e RUN_COMPLETE_FAILURE=false \
  -e VUS=1 \
  -e DURATION=30s \
  load-tests/scenarios/payment-monitoring.js
```

- `RUN_CREATE_ORDER=true` 는 실제 주문 생성 요청을 포함하므로 `ALLOW_STATE_CHANGE=true` 를 함께 명시한 경우에만 실행한다.

- 실행 후 Grafana Explore 또는 `MCJ Prod Payment Monitoring`에서 아래 PromQL을 확인한다.

```promql
sum by (source, result, reason) (increase(mcj_payment_approval_total{job="app"}[10m]))
sum by (result, reason) (increase(mcj_payment_order_created_total{job="app"}[10m]))
sum by (operation, result, status) (increase(mcj_portone_api_requests_total{job="app"}[10m]))
```

### 6.6 결제 오류 관측 절차
- dev 결제 오류는 `MCJ Dev Payment Monitoring`에서 먼저 확인한다.
- prod 결제 오류는 `MCJ Prod Payment Monitoring`에서 같은 패널 순서로 확인한다.
- 결제 완료 후 오류가 발생하면 HTTP 패널만 보지 말고, audit metric과 Loki 로그를 함께 확인한다.

1. `결제 완료 API 401/403 비율`, `결제 핵심 엔드포인트 5xx 비율`에서 HTTP 실패 여부를 확인한다.
2. `결제 승인 도메인 metric`, `PortOne API 성공/실패 도메인 metric`에서 승인 조회 실패인지 내부 처리 실패인지 구분한다.
3. `결제 audit 이벤트 추적 (10m)`에서 `source`, `event_type`, `result`, `reason` 조합을 확인한다.
4. `결제 audit/invalid webhook 로그`에서 같은 시간대의 `[payment_audit]` 또는 `[payment_webhook_invalid]` 로그를 확인한다.
5. `[payment_audit]` 로그는 `traceId`, `orderId`, `pgPaymentId`, `pgStatus`, `reason`으로 결제 처리 흐름을 좁힌다.
6. `[payment_webhook_invalid]` 로그는 `stage`, `hasWebhookId`, `hasWebhookTimestamp`, `hasWebhookSignature`로 signature/JSON 단계 실패를 좁힌다.

```promql
sum by (source, event_type, result, reason) (increase(mcj_payment_audit_events_total{job="app-dev"}[10m]))
sum by (source, event_type, result, reason) (increase(mcj_payment_audit_events_total{job="app"}[10m]))
```

```bash
{service="moongchijang-be",env="dev"} |~ "\\[payment_audit\\]|\\[payment_webhook_invalid\\]"
{service="moongchijang-be",env="prod"} |~ "\\[payment_audit\\]|\\[payment_webhook_invalid\\]"
```

### 6.7 dev 결제 synthetic metric
- synthetic metric은 실결제 전 대시보드 표시 검증이 필요할 때만 명시적으로 켠다.
- 기본값은 OFF이며, 실제 dev 결제 테스트 중에는 켜지 않는다.
- synthetic metric은 Micrometer counter/timer만 기록하며, 결제 주문/결제/audit log 테이블에 데이터를 쓰지 않고 Discord 알림도 발송하지 않는다.
- prod 프로필에는 synthetic metric 설정이 없고, 컴포넌트도 `dev`, `local`, `local-demo` 프로필에서만 생성된다.

```bash
PAYMENT_SYNTHETIC_METRICS_ENABLED=true
PAYMENT_SYNTHETIC_METRICS_FIXED_DELAY_MS=60000
PAYMENT_SYNTHETIC_METRICS_INITIAL_DELAY_MS=10000
```

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
