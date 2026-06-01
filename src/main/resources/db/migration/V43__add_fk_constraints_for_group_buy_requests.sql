ALTER TABLE group_buy_requests
    ADD CONSTRAINT fk_group_buy_requests_user_id
        FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE group_buy_request_status_histories
    ADD CONSTRAINT fk_group_buy_request_status_histories_request_id
        FOREIGN KEY (group_buy_request_id) REFERENCES group_buy_requests (id);

ALTER TABLE group_buy_open_requests
    ADD CONSTRAINT fk_group_buy_open_requests_user_id
        FOREIGN KEY (user_id) REFERENCES users (id);
