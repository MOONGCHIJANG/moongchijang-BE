-- 공구 검색용 ngram parser 기반 FULLTEXT 인덱스 추가
-- 대상:
--   - group_buys.product_name      : 상품명 검색
--   - stores.name, stores.address  : 매장명 + 주소(동 단위) 검색
-- stores.region/district는 enum name(VARCHAR)으로 저장되어 한글 검색어와 매칭되지 않으므로 비대상.
-- 검색어의 동(예: "성수")은 stores.address의 한글 주소 텍스트에서 매칭된다.
-- ngram_token_size는 MySQL 기본값(2)을 사용한다.

ALTER TABLE group_buys
    ADD FULLTEXT INDEX idx_group_buys_product_name (product_name) WITH PARSER ngram;

ALTER TABLE stores
    ADD FULLTEXT INDEX idx_stores_name_address (name, address) WITH PARSER ngram;
