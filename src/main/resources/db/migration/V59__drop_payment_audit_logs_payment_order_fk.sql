-- 결제 승인 트랜잭션이 payment_orders 행에 X-lock을 잡은 상태에서
-- REQUIRES_NEW 로 분리된 PaymentAuditLogService 가 payment_audit_logs INSERT 시
-- FK 검증을 위해 동일 행의 S-lock 을 기다리며 자기 자신과 락 대기에 빠지는 문제 해결.
-- payment_audit_logs.order_id (문자열) 로 추적 가능하므로 FK 강제는 불필요.

ALTER TABLE payment_audit_logs DROP FOREIGN KEY fk_payment_audit_logs_payment_order_id;
