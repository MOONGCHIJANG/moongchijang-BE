# 결제 동시성 실험 환경 및 데이터 준비 가이드

## 1. 목적

이 문서는 결제 완료/웹훅/환불 정합성 관련 동시성 실험을 로컬에서 재현하기 위한 환경, 데이터, 실행 순서를 정리한다.

실험의 핵심 목표는 다음과 같다.

- 동일한 MySQL, Redis를 공유하는 앱 인스턴스 2개에서 경쟁 상황을 만든다.
- 결제 승인 경로의 분산락, DB락, 조건부 update, 중복 처리 방어를 구성별로 비교한다.
- 실험 전후 상태를 동일하게 맞춰 재현 가능한 방식으로 결과를 기록한다.

## 2. 실험 환경

### 2.1 구성

- DB: MySQL (`localhost:3306`)
- Redis: Redis (`localhost:6379`)
- 앱 인스턴스 1: `localhost:8081`
- 앱 인스턴스 2: `localhost:8082`
- 관리 포트:
  - 인스턴스 1: `9081`
  - 인스턴스 2: `9082`

두 앱 인스턴스는 같은 MySQL, 같은 Redis를 바라봐야 한다.

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

### 2.4 확인 항목

- MySQL 컨테이너가 정상 기동했는지
- Redis 컨테이너가 정상 기동했는지
- 앱 2개가 모두 정상 기동했는지
- Flyway 마이그레이션이 정상 완료되었는지
- 두 앱이 같은 DB/Redis를 바라보는지

## 3. 데이터 준비 전략

### 3.1 기본 원칙

- 공구/매장/사용자 등 기본 참조 데이터는 DB에 직접 준비한다.
- `payment_orders`는 직접 insert하지 않고 주문 생성 API로 만든다.

주문을 API로 생성하는 이유는 다음과 같다.

- 서비스가 실제 `orderId(paymentId)`를 생성한다.
- 도메인 검증을 통과한 주문만 실험에 사용하게 된다.
- 실험 흐름이 실제 서비스 호출 순서와 더 가깝다.

추가로 현재 구현에서는 `validateNotParticipated()`가 `participation` 존재 여부를 기준으로 동작한다.
즉, 승인 전 `READY` 상태 주문은 같은 유저로 여러 건 생성 가능하다.
1차 실험에서는 이 점을 활용해 `buyer 1명 + 같은 공구에 READY 주문 여러 건` 전략으로 시작할 수 있다.

### 3.2 최소 데이터셋

실험용 최소 데이터셋은 아래 축으로 준비한다.

- `stores` 1건
- `group_buys` 1건
- 로그인 가능한 기존 `BUYER` 계정 1건 이상
- 필요 시 `user_role_assignments`

## 4. 최소 테이블 관계

실험 경로 기준 최소 관계는 아래 정도로 본다.

- `stores` -> `group_buys`
- `users` -> `payment_orders`
- `group_buys` -> `payment_orders`
- `payment_orders` -> `payments`
- `group_buys`, `users` -> `participations`

전체 ERD를 모두 사용할 필요는 없고, 실험용 공구 1건과 주문 생성/승인 흐름에 필요한 최소 부모 관계만 맞추면 된다.

## 5. 테이블별 최소 준비 항목

### 5.1 `stores`

실험용 매장 1건이 필요하다.

최소 준비 컬럼:

- `name`
- `address`
- `region`
- `district`

선택 컬럼:

- `phone_number`
- `latitude`
- `longitude`

### 5.2 `group_buys`

실험 대상 공구 1건이 필요하다.

목표 상태:

- `status = IN_PROGRESS`
- `current_quantity = 0`
- `target_quantity = [실험값]`
- `max_quantity = 100`

최소 준비 컬럼:

- `store_id`
- `product_name`
- `product_description`
- `price`
- `target_quantity`
- `current_quantity`
- `max_quantity`
- `status`
- `recruitment_start_at`
- `deadline`
- `pickup_date`
- `pickup_time_start`
- `pickup_time_end`
- `pickup_location`
- `order_status`

실험용 권장 조건:

- `deadline`은 실험 시점보다 충분히 뒤로 설정
- `recruitment_start_at`은 이미 시작된 시점으로 설정
- `per_user_limit`은 실험 의도에 맞게 충분히 크게 주거나 `null`로 둠

### 5.3 `users`

주문 생성 API를 여러 번 호출할 구매자 계정이 필요하다.

권장 방식:

- SQL로 새 사용자를 직접 넣기보다, 이미 로그인 가능한 로컬 `BUYER` 계정을 재사용한다.

이유:

- `password_hash`는 `BCryptPasswordEncoder`로 생성된다.
- `email_hash`는 별도 해시 로직으로 생성된다.
- 실험의 핵심은 인증 재현이 아니라 결제 승인 동시성 재현이다.

실무적으로는 아래 순서가 가장 단순하다.

1. 로컬에서 회원가입 또는 기존 계정으로 로그인 가능 여부 확인
2. 그 계정의 `user_id` 확인
3. 그 `user_id`로 주문 생성 API를 반복 호출

주의:

- 현재 구현 기준으로는 `participation`이 생성되기 전까지 같은 유저가 같은 공구에 대해 여러 `READY` 주문을 만들 수 있다.
- 따라서 1차 실험은 유저 1명으로도 시작 가능하다.

### 5.4 `user_role_assignments`

권한 체크가 역할 할당 기준으로 동작하는 경우를 대비해 buyer role 할당을 같이 맞춘다.

최소 준비 항목:

- `user_id`
- `role = BUYER`

이미 사용 중인 로컬 계정에 `BUYER` 역할이 정상 부여되어 있다면 추가 insert가 필요 없을 수 있다.

## 6. 주문 준비 방식

실험용 주문은 DB 직접 insert 대신 아래 API로 생성한다.

- `POST /api/v1/group-buys/{groupBuyId}/payment-orders`

요청 예시 필드:

- `quantity`
- `agreedNoCancelAfterGoal`
- `agreedRefundBeforeGoal`
- `agreedNoRefundAfterNoShow`
- `agreedNoWithdrawal`

실험 준비 단계에서 해야 할 일:

1. 두 앱에 override를 주입한다.
2. `reset.sql`로 이전 실험 흔적을 지운다.
3. 실험 준비 API로 유저/주문을 자동 생성한다.
4. 하네스가 반환된 `accessToken + paymentId + amount` 조합으로 동시 요청을 보낸다.

중요:

- 로컬에서는 실제 PortOne 결제 조회가 불가능하므로, `complete`/`webhook` 실험 전에는 실험용 override API로 fake PG 응답을 켜야 한다.
- fake PG 응답을 켜면 외부 PG 조회 대신 `PAID` 상태의 가짜 결제 결과를 반환하고, 승인 이후 정합성 로직만 검증할 수 있다.

관련 파일:

- 시드 SQL: `docs/payment-concurrency-experiment/seed.sql`
- 초기화 SQL: `docs/payment-concurrency-experiment/reset.sql`

## 6.1 빈 로컬 DB에서 실험용 사용자 1명 만들기

로컬 DB가 비어 있다면, 가장 처음 한 번은 실험용 기본 계정 1개를 만들어 두는 편이 편하다.

이 프로젝트는 사용자 생성 시 아래 값을 애플리케이션 로직으로 만든다.

- `password_hash`: `BCryptPasswordEncoder`
- `email_hash`: `HmacSHA256` 기반 해시

따라서 `users`를 SQL로 직접 넣기보다, 인증 API를 통해 실험용 계정을 만드는 편이 안전하다.

준비 조건:

- 앱 1개 이상 기동 완료
- Redis 기동 완료

권장 이메일:

- `experiment-buyer@moongchijang.local`

권장 비밀번호:

- `abc12345`

비밀번호 규칙:

- 8~20자
- 영문 + 숫자 포함
- 이메일 아이디와 동일하면 안 됨

### 6.1.1 인증코드 발송

```bash
curl -X POST http://localhost:8081/api/v1/auth/email/verification-codes \
  -H "Content-Type: application/json" \
  -d '{"email":"experiment-buyer@moongchijang.local"}'
```

### 6.1.2 Redis에서 인증코드 확인

로컬 환경에서 실제 메일 발송을 붙이지 않았다면, Redis에 저장된 인증코드를 직접 확인한다.

```bash
docker exec -it moongchijang-redis-local redis-cli \
  GET "auth:email:verification:code:experiment-buyer@moongchijang.local"
```

위 명령 결과의 6자리 숫자를 다음 단계에 사용한다.

### 6.1.3 인증코드 검증 후 signupToken 획득

```bash
curl -X POST http://localhost:8081/api/v1/auth/email/verification-codes/verify \
  -H "Content-Type: application/json" \
  -d '{"email":"experiment-buyer@moongchijang.local","code":"123456"}'
```

응답의 `signupToken` 값을 복사한다.

### 6.1.4 이메일 회원가입

```bash
curl -X POST http://localhost:8081/api/v1/auth/email/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"experiment-buyer@moongchijang.local","password":"abc12345","signupToken":"발급받은 signupToken"}'
```

성공하면 응답에서 아래 값을 확보한다.

- `accessToken`
- `user.id`

이후 실험 준비 단계에서는:

- `user.id`를 `docs/payment-concurrency-experiment/seed.sql`의 `@experiment_user_id`에 반영
- 이후 다중 유저/주문 준비는 실험 준비 API가 자동으로 처리한다.

## 6.2 실험 override 주입

실험 시작 전에는 `8081`, `8082` 두 앱에 같은 override를 주입해야 한다.

엔드포인트:

- `POST /internal/experiments/payment-overrides`
- `DELETE /internal/experiments/payment-overrides`

`FULL_PROTECTION` 예시:

```bash
curl -X POST http://localhost:8081/internal/experiments/payment-overrides \
  -H "Content-Type: application/json" \
  -d '{
    "enabled": true,
    "scenarioName": "full-protection",
    "distributedLockEnabled": true,
    "dbLockEnabled": true,
    "shortCircuitEnabled": true,
    "lockLeaseMs": 55000,
    "sleepBeforeCommitMs": 0,
    "fakePgEnabled": true,
    "fakePgStatus": "PAID",
    "fakePgMethod": "CARD"
  }'
```

`8082`에도 같은 요청을 보낸다. 다른 시나리오에서는 `scenarioName`과 락 관련 필드만 바꾸고, 로컬 실험에서는 `fakePgEnabled = true`를 유지한다.

초기화 예시:

```bash
curl -X DELETE http://localhost:8081/internal/experiments/payment-overrides
curl -X DELETE http://localhost:8082/internal/experiments/payment-overrides
```

## 6.3 유저/주문 자동 준비

수동으로 여러 유저를 회원가입시키고 로그인하는 대신, 실험 준비 API가 아래를 자동으로 처리한다.

- 실험용 이메일 사용자 N명 조회 또는 생성
- 각 사용자 access token 발급
- 각 사용자별 주문 1건 생성
- 하네스가 바로 사용할 `accessToken + paymentId + amount` 반환

엔드포인트:

- `POST /internal/experiments/payment-preparation`

예시:

```bash
curl -X POST http://localhost:8081/internal/experiments/payment-preparation \
  -H "Content-Type: application/json" \
  -d '{
    "groupBuyId": 1,
    "userCount": 20,
    "quantityPerOrder": 1
  }'
```

응답에는 아래 정보가 들어 있다.

- `userId`
- `email`
- `accessToken`
- `paymentId`
- `amount`

실제 실험에서는 테스트 코드가 이 엔드포인트를 먼저 호출한 뒤, 응답의 각 항목을 동시 요청 입력으로 사용한다.

## 7. 실험 전 상태 기준

각 실험 시작 전 아래 상태를 보장해야 한다.

- 대상 공구 상태가 `IN_PROGRESS`
- 대상 공구 `current_quantity = 0`
- 이전 실험의 `participations`가 남아있지 않음
- 이전 실험의 `payments`가 남아있지 않음
- 이전 실험의 `payment_orders`가 남아있지 않거나 이번 실험용 데이터로 새로 준비됨

## 8. 실험 후 확인 항목

각 구성 실행 후 최소 아래 값을 확인한다.

- `group_buys.current_quantity`
- 승인된 주문 수
- 생성된 참여 수
- 동일 주문의 상태 전이 횟수
- 로그 상 락 우회/락 획득/지연 삽입 여부

## 9. 권장 실행 순서

1. `FULL_PROTECTION`
2. `DISTRIBUTED_LOCK_ONLY`
3. `ALL_OFF`
4. `LOCK_RELEASED_BEFORE_COMMIT`

처음에는 소수 주문으로 전체 파이프라인이 정상 동작하는지 먼저 확인한 뒤, 준비 API의 `userCount`를 늘려가는 방식을 권장한다.

## 10. 초기화 포인트

매 실험 전후로 아래 정리를 수행할 수 있어야 한다.

- `group_buys.current_quantity` 초기화
- 공구 상태 `IN_PROGRESS` 복구
- 관련 `participations` 삭제
- 관련 `payments` 삭제
- 관련 `payment_orders` 삭제 또는 재생성

초기화 SQL은 FK 순서를 고려해 별도로 정리한다.

## 11. 직접 확인 및 보완이 필요한 값

아래 값은 실제 로컬 데이터 상태에 맞춰 채워야 한다.

- 실험용 `groupBuyId`
- 실험용 buyer user 수 (`payment-preparation.userCount`)
- `target_quantity`
- 주문 생성에 사용할 `quantity` (`payment-preparation.quantityPerOrder`)
- 실험 대상 공구의 `deadline`, `pickup_date`, `pickup_time_*`
- 초기화 대상 row 범위를 구분할 실험용 식별 규칙
