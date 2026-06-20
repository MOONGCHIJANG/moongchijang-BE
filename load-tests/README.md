# MCJ Load Tests

이 디렉터리는 MCJ 서비스의 부하테스트 스크립트와 실행 가이드를 관리합니다.

## 디렉터리 구조

- `config/`: 공통 옵션, 환경별 설정, threshold 정의
- `lib/`: 인증 헤더, 환경변수, 공통 요청 유틸
- `scenarios/`: 실제 시나리오 스크립트
- `templates/`: 신규 시나리오 작성용 템플릿
- `results/`: 로컬 실행 결과 저장 경로

## 실행 원칙

- 운영 환경(`prod`) 직접 실행은 기본 대상에서 제외합니다.
- staging 성격 환경이 있으면 해당 환경을 우선 사용합니다.
- staging 성격 환경이 없으면 제한된 `dev` 환경에서만 수행합니다.
- 테스트 시간대, 데이터, 알림 채널 영향 여부를 확인한 뒤 실행합니다.

## 시나리오 구분

| 시나리오 | 성격 | 기본 목적 | 상태 변경 가능성 |
| --- | --- | --- | --- |
| `group-buy-read` | 조회성 | 목록/상세/진행률 응답속도 확인 | 없음 |
| `mypage-read` | 조회성 | 참여/환불/개설 요청 조회 응답 확인 | 없음 |
| `admin-read` | 조회성 | 운영 조회성 화면 응답 확인 | 없음 |
| `favorite-stateful-read` | 상태성 | 찜/heartbeat 관련 부하 확인 | 낮음 |
| `payment-monitoring` | 결제 모니터링 | 실패 흐름 및 도메인 metric 노출 확인 | 기본 없음, 옵션 시 있음 |

## 사전 준비

### 1. k6 설치

로컬 또는 실행 환경에 `k6`가 설치되어 있어야 합니다.

### 2. 환경변수 준비

아래 환경변수를 기본 사용합니다.

- `MCJ_BASE_URL`: 대상 API base URL
- `MCJ_ENV_NAME`: 대상 환경 식별자 (`dev`, `staging` 등)
- `MCJ_ACCESS_TOKEN`: 인증이 필요한 시나리오용 토큰
- `MCJ_ADMIN_ACCESS_TOKEN`: 관리자 권한 시나리오용 토큰
- `MCJ_SCENARIO_NAME`: 실행 시나리오 이름
- `MCJ_GROUP_BUY_ID`: 상세/진행률 조회 대상 공구 ID
- `MCJ_REPORT_YEAR`: 운영 조회 시나리오 대상 연도
- `MCJ_REPORT_MONTH`: 운영 조회 시나리오 대상 월
- `ALLOW_STATE_CHANGE`: 쓰기성 요청 허용 여부
- `RUN_CREATE_ORDER`: 결제 주문 생성 요청 실행 여부
- `RUN_COMPLETE_FAILURE`: 존재하지 않는 주문 기준 결제 실패 흐름 실행 여부
- `RUN_WEBHOOK_INVALID`: 잘못된 웹훅 payload 흐름 실행 여부

예시:

```bash
export MCJ_BASE_URL=https://api.moongchijang.com/dev
export MCJ_ENV_NAME=dev
export MCJ_ACCESS_TOKEN=<ACCESS_TOKEN>
export MCJ_GROUP_BUY_ID=960005
```

## 실행 기준 요약

### 1. 공통 규칙

- 모든 실행은 `MCJ_SCENARIO_NAME` 값을 명시합니다.
- 결과 저장이 필요한 경우 `--summary-export` 경로를 함께 지정합니다.
- 기본값이 있더라도 대상 ID, 토큰, 환경명은 실행 전에 다시 확인합니다.

### 2. 결제 시나리오 규칙

- 기본 실행은 `RUN_COMPLETE_FAILURE=true` 기준의 실패 흐름 확인입니다.
- 실제 주문 생성이 포함되는 경우 `RUN_CREATE_ORDER=true` 와 `ALLOW_STATE_CHANGE=true` 를 함께 사용합니다.
- 쓰기성 실행 시 대상 공구 ID, 실행 회차, 결과 문서 경로를 함께 기록합니다.
- 결제 결과 확인은 `MCJ Dev Payment Monitoring` 또는 `MCJ Prod Payment Monitoring` 기준으로 수행합니다.

## 기본 실행 예시

시나리오가 추가되면 아래 형식으로 실행합니다.

```bash
k6 run load-tests/scenarios/<scenario-file>.js
```

공동구매 조회 시나리오 예시:

```bash
MCJ_SCENARIO_NAME=group-buy-read \
MCJ_GROUP_BUY_ID=960005 \
k6 run load-tests/scenarios/group-buy-read.js
```

마이페이지 조회 시나리오 예시:

```bash
MCJ_SCENARIO_NAME=mypage-read \
MCJ_ACCESS_TOKEN=<ACCESS_TOKEN> \
k6 run load-tests/scenarios/mypage-read.js
```

관리자 조회 시나리오 예시:

```bash
MCJ_SCENARIO_NAME=admin-read \
MCJ_ADMIN_ACCESS_TOKEN=<ADMIN_ACCESS_TOKEN> \
MCJ_REPORT_YEAR=2026 \
MCJ_REPORT_MONTH=6 \
k6 run load-tests/scenarios/admin-read.js
```

찜/상태성 조회 시나리오 예시:

```bash
MCJ_SCENARIO_NAME=favorite-stateful-read \
MCJ_ACCESS_TOKEN=<ACCESS_TOKEN> \
MCJ_GROUP_BUY_ID=960005 \
k6 run load-tests/scenarios/favorite-stateful-read.js
```

결제 모니터링 시나리오 예시:

```bash
MCJ_SCENARIO_NAME=payment-monitoring \
MCJ_BASE_URL=https://api.moongchijang.com/dev \
MCJ_ACCESS_TOKEN=<ACCESS_TOKEN> \
k6 run load-tests/scenarios/payment-monitoring.js
```

결제 주문 생성까지 확인할 때는 아래 옵션을 추가합니다.

```bash
MCJ_SCENARIO_NAME=payment-monitoring \
MCJ_BASE_URL=https://api.moongchijang.com/dev \
MCJ_ACCESS_TOKEN=<ACCESS_TOKEN> \
MCJ_GROUP_BUY_ID=960005 \
ALLOW_STATE_CHANGE=true \
RUN_CREATE_ORDER=true \
RUN_COMPLETE_FAILURE=false \
k6 run load-tests/scenarios/payment-monitoring.js
```

`RUN_CREATE_ORDER=true` 는 실제 주문 생성 요청이 포함되므로 `ALLOW_STATE_CHANGE=true` 를 함께 명시할 때만 실행됩니다.

결과를 파일로 저장하려면:

```bash
k6 run --summary-export=load-tests/results/<scenario-name>-summary.json load-tests/scenarios/<scenario-file>.js
```

표준 예시:

```bash
MCJ_SCENARIO_NAME=payment-monitoring \
MCJ_BASE_URL=https://api.moongchijang.com/dev \
k6 run \
  --summary-export=load-tests/results/2026-06-16-payment-monitoring-summary.json \
  load-tests/scenarios/payment-monitoring.js
```

## 시나리오별 필수 환경변수

| 시나리오 | 필수 환경변수 | 선택 환경변수 |
| --- | --- | --- |
| `group-buy-read` | `MCJ_SCENARIO_NAME`, `MCJ_BASE_URL`, `MCJ_GROUP_BUY_ID` | `MCJ_ENV_NAME` |
| `mypage-read` | `MCJ_SCENARIO_NAME`, `MCJ_BASE_URL`, `MCJ_ACCESS_TOKEN` | `MCJ_ENV_NAME` |
| `admin-read` | `MCJ_SCENARIO_NAME`, `MCJ_BASE_URL`, `MCJ_ADMIN_ACCESS_TOKEN`, `MCJ_REPORT_YEAR`, `MCJ_REPORT_MONTH` | `MCJ_ENV_NAME` |
| `favorite-stateful-read` | `MCJ_SCENARIO_NAME`, `MCJ_BASE_URL`, `MCJ_ACCESS_TOKEN`, `MCJ_GROUP_BUY_ID` | `MCJ_ENV_NAME` |
| `payment-monitoring` | `MCJ_SCENARIO_NAME`, `MCJ_BASE_URL` | `MCJ_ACCESS_TOKEN`, `MCJ_GROUP_BUY_ID`, `RUN_CREATE_ORDER`, `RUN_COMPLETE_FAILURE`, `RUN_WEBHOOK_INVALID`, `ALLOW_STATE_CHANGE` |

## 구현 순서

1. 조회성 baseline 시나리오 추가
2. 결제 주문 생성/승인 시나리오 추가
3. 결제 취소/웹훅 시나리오 추가
4. 마이페이지/운영 조회 시나리오 추가
5. 결과 기록 템플릿 및 운영 확인 가이드 추가

## 참고 문서

- `docs/load-test-strategy.md`
- `docs/load-test-execution-checklist.md`
- `docs/load-test-result-template.md`
