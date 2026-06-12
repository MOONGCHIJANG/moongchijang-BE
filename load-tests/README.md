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

## 사전 준비

### 1. k6 설치

로컬 또는 실행 환경에 `k6`가 설치되어 있어야 합니다.

### 2. 환경변수 준비

아래 환경변수를 기본 사용합니다.

- `MCJ_BASE_URL`: 대상 API base URL
- `MCJ_ENV_NAME`: 대상 환경 식별자 (`dev`, `staging` 등)
- `MCJ_ACCESS_TOKEN`: 인증이 필요한 시나리오용 토큰
- `MCJ_SCENARIO_NAME`: 실행 시나리오 이름
- `MCJ_GROUP_BUY_ID`: 상세/진행률 조회 대상 공구 ID

예시:

```bash
export MCJ_BASE_URL=https://api.moongchijang.com/dev
export MCJ_ENV_NAME=dev
export MCJ_ACCESS_TOKEN=<ACCESS_TOKEN>
export MCJ_GROUP_BUY_ID=960005
```

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

결과를 파일로 저장하려면:

```bash
k6 run --summary-export=load-tests/results/<scenario-name>-summary.json load-tests/scenarios/<scenario-file>.js
```

## 구현 순서

1. 조회성 baseline 시나리오 추가
2. 결제 주문 생성/승인 시나리오 추가
3. 결제 취소/웹훅 시나리오 추가
4. 마이페이지/운영 조회 시나리오 추가
5. 결과 기록 템플릿 및 운영 확인 가이드 추가

## 참고 문서

- `docs/load-test-strategy.md`
