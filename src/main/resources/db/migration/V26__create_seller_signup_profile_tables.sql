CREATE TABLE seller_business_profiles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    business_registration_number VARCHAR(20) NOT NULL,
    store_name VARCHAR(100) NOT NULL,
    owner_name VARCHAR(50) NOT NULL,
    store_address VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_seller_business_profiles_user_id
        FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE UNIQUE INDEX uidx_seller_business_profiles_user_id ON seller_business_profiles (user_id);
CREATE UNIQUE INDEX uidx_seller_business_profiles_registration_no ON seller_business_profiles (business_registration_number);

CREATE TABLE seller_settlement_accounts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    bank_code VARCHAR(30) NOT NULL,
    account_number VARCHAR(50) NOT NULL,
    account_holder_name VARCHAR(50) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_seller_settlement_accounts_user_id
        FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE UNIQUE INDEX uidx_seller_settlement_accounts_user_id ON seller_settlement_accounts (user_id);
