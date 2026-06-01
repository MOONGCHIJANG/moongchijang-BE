ALTER TABLE group_buys
    DROP FOREIGN KEY fk_group_buys_request_id;

ALTER TABLE group_buys
    MODIFY group_buy_request_id BIGINT NULL;

ALTER TABLE group_buys
    ADD CONSTRAINT fk_group_buys_request_id
        FOREIGN KEY (group_buy_request_id) REFERENCES group_buy_requests (id);
