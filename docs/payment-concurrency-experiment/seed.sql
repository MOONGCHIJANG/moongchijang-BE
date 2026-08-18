-- 결제 동시성 실험용 최소 시드 데이터
-- 실행 전제:
-- 1) Flyway 마이그레이션이 모두 끝난 로컬 MySQL
-- 2) 실험 전용 local 데이터베이스에서만 실행
--
-- 전략:
-- - stores 1건
-- - group_buys 1건
-- - 로그인 가능한 기존 BUYER 계정 1건 재사용
-- - group_buys 1건
--
-- 주문(payment_orders)은 이 SQL에서 직접 만들지 않는다.
-- 실험용 주문은 API(`/api/v1/group-buys/{groupBuyId}/payment-orders`)로 생성한다.
--
-- 중요:
-- createPaymentOrder()의 validateNotParticipated()는 participation 존재 여부만 확인한다.
-- 따라서 승인 전 READY 주문은 같은 유저로 여러 건 생성 가능하다.

START TRANSACTION;

SET @now = NOW();
SET @experiment_user_id = 0;

-- 1. 실험용 매장
INSERT INTO stores (
    name,
    address,
    phone_number,
    latitude,
    longitude,
    region,
    district,
    created_at,
    updated_at
)
VALUES (
    '실험용 베이커리',
    '서울특별시 성동구 성수이로 100',
    '010-0000-0000',
    37.5440,
    127.0550,
    'SEOUL',
    'SEOUL_SEONGSU_GEONDAE_GWANGJIN',
    @now,
    @now
);

SET @experiment_store_id = LAST_INSERT_ID();

-- 2. 실험용 구매자
-- users는 SQL로 직접 넣지 않고, 이미 로그인 가능한 local BUYER 계정을 재사용한다.
-- 아래 값만 실제 user id로 바꿔서 사용한다.
--
-- 예시:
-- SET @experiment_user_id = 123;
--
-- 이유:
-- - password_hash는 BCrypt로 저장되어 직접 고정값을 넣기 어렵다.
-- - email_hash는 별도 해시 로직을 따라야 한다.
-- - 실험 목적은 인증 재현이 아니라 결제 승인 동시성 재현이므로,
--   로그인 가능한 기존 계정을 재사용하는 편이 안전하다.

-- 3. 실험용 공구
-- target_quantity와 max_quantity는 실험 의도에 맞게 조정 가능하다.
INSERT INTO group_buys (
    store_id,
    group_buy_request_id,
    thumbnail_key,
    product_name,
    product_description,
    price,
    original_price,
    target_quantity,
    current_quantity,
    max_quantity,
    per_user_limit,
    status,
    recruitment_start_at,
    deadline,
    pickup_date,
    pickup_time_start,
    pickup_time_end,
    pickup_location,
    pickup_contact,
    share_count,
    close_reason,
    close_reason_detail,
    close_requested_at,
    close_request_review_status,
    close_request_rejection_reason,
    close_reviewed_at,
    closed_by_type,
    achieved_at,
    order_status,
    order_owner_contacted_at,
    order_confirmed_at,
    order_cancelled_at,
    created_at,
    updated_at
)
VALUES (
    @experiment_store_id,
    NULL,
    'local/experiments/payment-concurrency/thumbnail.jpg',
    '실험용 크루아상 세트',
    '결제 동시성 실험을 위한 로컬 공구 데이터',
    12000,
    15000,
    50,
    0,
    100,
    NULL,
    'IN_PROGRESS',
    DATE_SUB(@now, INTERVAL 1 DAY),
    DATE_ADD(@now, INTERVAL 7 DAY),
    DATE_ADD(CURDATE(), INTERVAL 8 DAY),
    '14:00:00',
    '16:00:00',
    '서울특별시 성동구 성수이로 100',
    '010-0000-0000',
    0,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    'PENDING',
    NULL,
    NULL,
    NULL,
    @now,
    @now
);

SET @experiment_group_buy_id = LAST_INSERT_ID();

COMMIT;

-- 확인용
SELECT @experiment_store_id AS experiment_store_id;
SELECT @experiment_user_id AS experiment_user_id;
SELECT @experiment_group_buy_id AS experiment_group_buy_id;
