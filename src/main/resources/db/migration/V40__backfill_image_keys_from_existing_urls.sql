-- 기존 CDN/S3 전체 URL 데이터에서 image key를 역추출해 백필한다.
-- 대상: key 컬럼이 비어 있고 URL이 http/https로 저장된 레코드

UPDATE group_buys
SET thumbnail_key = SUBSTRING_INDEX(
    SUBSTRING(thumbnail_url, LOCATE('/', thumbnail_url, LOCATE('://', thumbnail_url) + 3) + 1),
    '?',
    1
)
WHERE (thumbnail_key IS NULL OR thumbnail_key = '')
  AND thumbnail_url IS NOT NULL
  AND thumbnail_url REGEXP '^https?://'
  AND LOCATE('/', thumbnail_url, LOCATE('://', thumbnail_url) + 3) > 0;

UPDATE group_buy_images
SET image_key = SUBSTRING_INDEX(
    SUBSTRING(image_url, LOCATE('/', image_url, LOCATE('://', image_url) + 3) + 1),
    '?',
    1
)
WHERE (image_key IS NULL OR image_key = '')
  AND image_url IS NOT NULL
  AND image_url REGEXP '^https?://'
  AND LOCATE('/', image_url, LOCATE('://', image_url) + 3) > 0;

UPDATE owner_group_buy_requests
SET thumbnail_key = SUBSTRING_INDEX(
    SUBSTRING(thumbnail_url, LOCATE('/', thumbnail_url, LOCATE('://', thumbnail_url) + 3) + 1),
    '?',
    1
)
WHERE (thumbnail_key IS NULL OR thumbnail_key = '')
  AND thumbnail_url IS NOT NULL
  AND thumbnail_url REGEXP '^https?://'
  AND LOCATE('/', thumbnail_url, LOCATE('://', thumbnail_url) + 3) > 0;

UPDATE owner_group_buy_request_images
SET image_key = SUBSTRING_INDEX(
    SUBSTRING(image_url, LOCATE('/', image_url, LOCATE('://', image_url) + 3) + 1),
    '?',
    1
)
WHERE (image_key IS NULL OR image_key = '')
  AND image_url IS NOT NULL
  AND image_url REGEXP '^https?://'
  AND LOCATE('/', image_url, LOCATE('://', image_url) + 3) > 0;
