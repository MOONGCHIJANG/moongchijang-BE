# 결제 동시성 실험 환경 및 데이터 준비 가이드

## 1. 목적

이 문서는 결제 완료 경로의 동시성 실험을 로컬에서 재현할 때 사용한 환경, 데이터, 실행 순서를 정리한다.

정리 대상은 아래 두 가지다.

- 실험을 위해 실제로 맞췄던 로컬 환경
- 실험 과정에서 얻은 운영 개선 포인트와 임시 보조 도구의 경계

## 2. 공통 환경

### 2.1 구성

- DB: MySQL (`localhost:3306`)
- Redis: Redis (`localhost:6379`)
- 앱 인스턴스 1: `localhost:8081`
- 앱 인스턴스 2: `localhost:8082`
- 관리 포트:
  - 인스턴스 1: `9081`
  - 인스턴스 2: `9082`

두 인스턴스는 같은 MySQL, 같은 Redis를 바라보도록 맞춘다.

### 2.2 인프라 기동

```bash
cd docker
docker compose --profile local up -d mysql-local redis-local
```

### 2.3 앱 기동

터미널 1:

```bash
cd ..
./gradlew bootRun --args='--server.port=8081 --management.server.port=9081'
```

터미널 2:

```bash
cd ..
./gradlew bootRun --args='--server.port=8082 --management.server.port=9082'
```

## 3. 메인에 남겨둔 운영 개선점

실험 브랜치에서 확인한 내용 중 운영 코드에 남겨둘 가치가 있는 부분만 메인 로직에 반영했다.

### 3.1 락 관련 환경변수

결제 완료 경로의 락 대기/임대/재시도 값은 환경변수로 조절할 수 있게 정리했다.

- `PAYMENT_COMPLETION_LOCK_WAIT_MS`
- `PAYMENT_COMPLETION_LOCK_LEASE_MS`
- `PAYMENT_COMPLETION_LOCK_RETRY_COUNT`
- `PAYMENT_COMPLETION_LOCK_RETRY_DELAY_MS`

이 값들은 `application-dev.yml`, `application-local-demo.yml`, `application-prod.yml`에서 읽도록 맞췄다.

### 3.2 락 재시도

단일 락 획득 실패를 바로 최종 실패로 두지 않고, 짧은 재시도 기회를 주는 로직을 남겨뒀다.

### 3.3 ACHIEVED 상태 수량 반영

목표 수량에 도달한 직후에도 나머지 승인 요청이 정상 반영될 수 있도록, 수량 증가 로직은 `ACHIEVED` 상태에서도 동작하도록 보완했다.

이 변경 이후에는 목표 수량 50을 찍은 뒤 남은 승인도 `max_quantity = 100` 범위 안에서 계속 반영된다.

## 4. 실험 브랜치에서만 사용한 보조 도구

실험 당시에는 반복적인 데이터 준비와 시나리오 전환을 빠르게 하기 위해 보조 API를 잠시 열어 사용했다.

- `POST /internal/experiments/payment-preparation`
- `POST /internal/experiments/payment-overrides`
- `DELETE /internal/experiments/payment-overrides`

이 API들은 실험 브랜치에서만 사용한 임시 도구다.

현재 메인 코드에서는 아래 기준으로 정리했다.

- 운영 가치가 있는 락/재시도/상태 처리 개선은 유지
- 실험 전용 컨트롤러, DTO, 보안 예외는 제거

즉, 메인에는 실험용 내부 엔드포인트가 남아 있지 않고, 실험 기록은 `docs`와 테스트 패키지 쪽에만 남긴 상태다.

테스트 패키지에 남아 있는 하네스와 클라이언트는 당시 실험 절차를 보존하기 위한 기록물로 본다. 메인에서 실험용 내부 엔드포인트를 제거했기 때문에, 지금 기준으로 같은 흐름을 그대로 다시 실행하려면 실험 브랜치의 보조 준비 로직이 함께 있어야 한다.

## 5. 데이터 준비 방식

### 5.1 기준 데이터

실험에서는 아래 축만 최소로 맞췄다.

- `stores` 1건
- `group_buys` 1건
- 로그인 가능한 기본 `BUYER` 계정

공구 상태는 아래처럼 시작했다.

- `status = IN_PROGRESS`
- `current_quantity = 0`
- `target_quantity = 50`
- `max_quantity = 100`

### 5.2 주문 준비

`payment_orders`는 직접 insert하지 않고 준비 로직 또는 주문 생성 API를 통해 만들었다.

이유는 아래와 같다.

- 실제 서비스가 생성하는 `orderId(paymentId)`를 그대로 사용하기 위해
- 서비스 검증을 통과한 주문만 실험에 포함하기 위해
- 승인 흐름을 실제 호출 순서에 가깝게 맞추기 위해

### 5.3 실험 단위

이번 실험은 “한 사용자의 중복 요청”보다 “여러 구매자의 동시 승인 경쟁”을 보는 쪽에 초점을 맞췄다.

그래서 `requestCount` 값에 맞춰 아래처럼 데이터를 준비했다.

- `20` 실험: 구매자 20명, 주문 20건
- `50` 실험: 구매자 50명, 주문 50건
- `100` 실험: 구매자 100명, 주문 100건

## 6. 실행 기록물

실험 결과를 재정리할 때는 아래 파일을 기준으로 본다.

- 결과 요약: [results.md](./results.md)
- 시드 데이터: [seed.sql](./seed.sql)
- 초기화 SQL: [reset.sql](./reset.sql)
- 정제 로그: [logs/README.md](./logs/README.md)

정제 로그는 비교에 필요한 핵심 라인만 남긴 버전이고, 원본 전체 로그는 로컬 확인용으로만 다뤘다.
