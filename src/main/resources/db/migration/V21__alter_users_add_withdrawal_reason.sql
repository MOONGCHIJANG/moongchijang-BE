ALTER TABLE users
ADD COLUMN withdrawal_reason VARCHAR(50) NULL,
ADD COLUMN withdrawal_reason_detail VARCHAR(500) NULL;
