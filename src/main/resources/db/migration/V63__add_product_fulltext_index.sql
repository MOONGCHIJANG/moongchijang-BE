-- 신규 상품 카탈로그 검색용 ngram FULLTEXT 인덱스.
-- 매장명·주소는 V13의 idx_stores_name_address를 계속 사용한다.
ALTER TABLE products
    ADD FULLTEXT INDEX idx_products_name (name) WITH PARSER ngram;
