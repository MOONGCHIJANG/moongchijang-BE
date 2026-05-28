ALTER TABLE users
ADD COLUMN owner_withdrawal_reason VARCHAR(50) NULL,
ADD COLUMN owner_withdrawal_reason_detail VARCHAR(500) NULL;
