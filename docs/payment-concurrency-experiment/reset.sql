-- 결제 동시성 실험 초기화 SQL
-- 전제:
-- - 아래 변수 값을 실제 실험용 공구/유저 ID에 맞게 채운다.
-- - 실험 대상 데이터만 정리해야 한다.

SET @experiment_group_buy_id = 0; -- TODO: 실험용 group_buy_id
SET @experiment_user_id = 0;      -- TODO: 실험용 user_id

START TRANSACTION;

-- 1. participation 제거
DELETE FROM participation
WHERE group_buy_id = @experiment_group_buy_id;

-- 2. payments 제거
DELETE p
FROM payments p
JOIN payment_orders po ON po.id = p.payment_order_id
WHERE po.group_buy_id = @experiment_group_buy_id;

-- 3. payment_orders 제거
DELETE FROM payment_orders
WHERE group_buy_id = @experiment_group_buy_id;

-- 4. group_buy 상태 원복
UPDATE group_buys
SET current_quantity = 0,
    status = 'IN_PROGRESS',
    achieved_at = NULL,
    order_status = 'PENDING',
    order_owner_contacted_at = NULL,
    order_confirmed_at = NULL,
    order_cancelled_at = NULL,
    close_reason = NULL,
    close_reason_detail = NULL,
    close_requested_at = NULL,
    close_request_review_status = NULL,
    close_request_rejection_reason = NULL,
    close_reviewed_at = NULL,
    closed_by_type = NULL,
    updated_at = NOW()
WHERE id = @experiment_group_buy_id;

COMMIT;

-- 확인용
SELECT id, status, current_quantity, achieved_at, order_status
FROM group_buys
WHERE id = @experiment_group_buy_id;
