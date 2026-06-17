# MCJ dev 부하테스트 모니터링 반영 가이드

## 1. 목적

이 문서는 dev 환경에서 부하테스트를 수행할 때 필요한 모니터링 반영 범위와 사전 점검 항목을 정리합니다.

부하테스트 시나리오가 준비되어 있어도, dev 메트릭이 수집되지 않거나 prod 알림과 동일 채널로 전송되면 테스트 결과 확인과 운영 알림 분리가 어렵습니다.

## 2. 현재 상태

현재 저장소 기준 모니터링 구성은 아래 상태입니다.

- Prometheus는 prod를 `job=app`, dev를 `job=app-dev`로 분리 수집하도록 구성합니다.
- Grafana 대시보드는 prod 기준 패널 구성이 기본입니다.
- Alertmanager 알림 라우팅도 prod 운영 알림 기준으로 구성되어 있습니다.
- `app-dev` 서비스는 `/dev` prefix로 라우팅되지만, dev 부하테스트용 메트릭 확인 절차는 별도 문서화되어 있지 않습니다.

## 3. dev 부하테스트 실행 전제

- 부하테스트 스크립트는 `develop` 기준 변경사항이 dev 환경에 배포된 이후 실행합니다.
- 테스트 대상 데이터와 토큰은 dev 환경 기준으로 준비합니다.
- 테스트 시간대는 일반 개발 작업과 겹치지 않도록 사전 조율합니다.
- 테스트 트래픽이 운영성 알림으로 오인되지 않도록 채널 또는 라우팅 분리를 우선 검토합니다.

## 3.1 dev 실행 원칙

- 기본 실행 환경은 `dev`로 고정합니다.
- 최초 실행은 조회성 또는 실패 흐름 중심 시나리오부터 시작합니다.
- 쓰기성 요청이 포함된 시나리오는 마지막 단계에서 제한적으로 실행합니다.
- 동일 날짜 재실행 시 결과 파일은 회차(`run2`, `run3`)를 구분합니다.

## 4. 반영 필요 항목

### 4.1 Prometheus 수집

- `app-dev:8081` 메트릭 수집 대상 반영
- dev / prod 구분용 별도 job 기준 사용 (`job=app`, `job=app-dev`)
- dev 스크랩 실패가 prod 경고로 오인되지 않도록 알람 조건 분리 검토

### 4.2 Grafana 대시보드

- dev 환경용 별도 대시보드(`MCJ Dev Load Test Monitoring`) 기준 사용
- 결제 지표는 별도 대시보드(`MCJ Dev Payment Monitoring`)에서 분리 확인
- 부하테스트 핵심 흐름 기준 패널 확인 가능 여부 점검
- 결제 도메인 metric 조회 시 `job=app-dev` 기준으로 분리 수집 여부 점검

### 4.3 Alertmanager / Discord 채널

- dev 테스트용 알림 채널 분리 검토
- prod 채널과 동일 알림 제목/prefix 사용 여부 검토
- 테스트 중 반복 알림 발생 시 mute 또는 임시 비활성화 절차 정리

### 4.4 실행/기록 기준

- 테스트 실행자, 실행 일시, 대상 브랜치 기록
- 사용 시나리오, 환경변수, 대상 공구/주문 데이터 기록
- Grafana 확인 패널, Prometheus 쿼리, 에러 로그 확인 위치 기록
- 결제 시나리오는 실패 흐름과 쓰기 흐름을 분리 기록
- 쓰기 흐름 실행 시 `ALLOW_STATE_CHANGE=true` 사용 여부 기록

### 4.5 결제 결과 기록 기준

- 기본 확인 대시보드는 `MCJ Dev Payment Monitoring`으로 통일
- 결과 기록에는 `payment_flow` 기준 실행 흐름을 함께 남김
- 도메인 metric 확인 시 `job=app-dev` 라벨 포함 쿼리 사용
- 실패 흐름 실행 시 운영 데이터 변경 여부를 함께 기록
- 주문 생성 포함 실행 시 대상 공구 ID와 실행 회차를 함께 기록

## 5. 권장 적용 순서

1. dev 메트릭 수집 범위 확인
2. dev 대시보드 확인 방식 확정
3. dev 결제 대시보드 분리 여부 확인
4. dev 알림 채널 또는 라우팅 분리 여부 확정
5. 부하테스트 실행 체크리스트 반영
6. 시나리오별 결과 기록 포맷 반영

## 6. 이번 브랜치 기준 활용 방식

이번 브랜치에서는 아래 순서로 활용합니다.

- `load-tests/` 시나리오 구현 진행
- 결제 모니터링 관련 PR 머지 여부 확인
- dev 일반/결제 모니터링 반영 범위 확정
- `develop` 배포 후 dev 환경에서 시나리오 실행
- 결과 기록 및 후속 개선사항 정리

## 6.1 dev 실행 절차

1. `develop` 기준 변경사항이 dev에 배포되었는지 확인합니다.
2. 실행 대상 시나리오와 대상 데이터를 확정합니다.
3. `MCJ_BASE_URL`, 토큰, 대상 ID, `MCJ_SCENARIO_NAME` 값을 준비합니다.
4. Prometheus `job=app-dev` 수집 상태와 Grafana 대시보드 노출 상태를 확인합니다.
5. 읽기 전용 또는 실패 흐름 시나리오를 먼저 실행합니다.
6. 결과를 `load-tests/results/` 경로에 저장합니다.
7. Grafana/Prometheus/로그를 확인하고 `docs/load-test-result-template.md` 기준으로 정리합니다.
8. 필요 시에만 쓰기성 시나리오를 제한적으로 재실행합니다.

## 6.2 권장 실행 순서

1. `group-buy-read`
2. `mypage-read`
3. `admin-read`
4. `favorite-stateful-read`
5. `payment-monitoring` 실패 흐름
6. `payment-monitoring` 주문 생성 포함 흐름

## 6.3 실행 명령 예시

### 조회성/실패 흐름 우선 실행

```bash
MCJ_BASE_URL=https://api.moongchijang.com/dev \
MCJ_ENV_NAME=dev \
MCJ_SCENARIO_NAME=payment-monitoring \
MCJ_ACCESS_TOKEN=<ACCESS_TOKEN> \
k6 run \
  --summary-export=load-tests/results/2026-06-16-payment-monitoring-summary.json \
  load-tests/scenarios/payment-monitoring.js
```

### 쓰기성 포함 재실행

```bash
MCJ_BASE_URL=https://api.moongchijang.com/dev \
MCJ_ENV_NAME=dev \
MCJ_SCENARIO_NAME=payment-monitoring \
MCJ_ACCESS_TOKEN=<ACCESS_TOKEN> \
MCJ_GROUP_BUY_ID=960005 \
ALLOW_STATE_CHANGE=true \
RUN_CREATE_ORDER=true \
RUN_COMPLETE_FAILURE=false \
k6 run \
  --summary-export=load-tests/results/2026-06-16-payment-monitoring-run2-summary.json \
  load-tests/scenarios/payment-monitoring.js
```

## 6.4 실행 직후 확인 순서

1. `MCJ Dev Load Test Monitoring` 또는 `MCJ Dev Payment Monitoring` 대시보드 확인
2. HTTP 4xx / 5xx, p95 / p99, RPS 변화 확인
3. 결제 시나리오의 경우 도메인 metric 증가 여부 확인
4. 애플리케이션 에러 로그 확인
5. 결과 Markdown 초안 작성

## 7. 결제 결과 기록 시 확인 항목

### 7.1 Grafana

- `MCJ Dev Payment Monitoring`
- 결제 시도/성공/실패 건수 (5m)
- 결제 완료 API p95/p99 latency
- 웹훅 처리 도메인 metric
- PortOne API 성공/실패 도메인 metric
- PortOne API latency p95 도메인 metric

### 7.2 Prometheus

```promql
sum by (source, result, reason) (increase(mcj_payment_approval_total{job="app-dev"}[10m]))
sum by (result, reason) (increase(mcj_payment_order_created_total{job="app-dev"}[10m]))
sum by (event_type, result, reason) (increase(mcj_payment_webhook_processed_total{job="app-dev"}[10m]))
sum by (operation, result, status) (increase(mcj_portone_api_requests_total{job="app-dev"}[10m]))
histogram_quantile(0.95, sum by (operation, le) (rate(mcj_portone_api_latency_seconds_bucket{job="app-dev"}[10m])))
```

### 7.3 파일 기록 규칙

- `load-tests/results/YYYY-MM-DD-<scenario-name>-summary.json`
- `load-tests/results/YYYY-MM-DD-<scenario-name>.md`
- 동일 날짜 재실행 시 `-run2`, `-run3` suffix 사용
