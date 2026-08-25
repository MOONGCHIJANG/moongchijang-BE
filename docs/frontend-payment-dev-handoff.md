# Frontend Payment Dev Handoff

작성일: 2026-05-19

## Figma 대조 결과

확인한 Figma node:

- file: `aSmUN95u8N3JE2kCSYz7nN`
- canvas: `1:27`
- 주요 화면:
  - `339:1374` 공구피드-1
  - `339:1543` 공구피드-상페-1
  - `339:1605` 공구피드-상페-1-스크롤1

피드 화면에서 확인한 표시 요소:

- 검색 placeholder: `매장명 또는 상품명 검색`
- 필터: `전체`, `마감임박`, `달성임박`, `서울`, `경기`
- 카드: 이미지, 찜, 공유, 가격, 참여 수량
- 예시 문구: `6,000원`, `36개/ 50개 참여중`

상세 화면에서 확인한 표시 요소:

- 대표 이미지
- 매장/지역: `LOAF · 성수동`
- 상품명, 가격
- 달성률: `72%`
- 참여/목표 수량: `참여 36 / 최소 50개`
- 마감 일시: `마감 4/10 23:59`
- 픽업 일시: `4/15(화) 14:00~18:00`
- 픽업 장소, 매장명
- 인당 최소: `1개`
- 상품 설명, 위치 섹션
- 공유, 찜, 참여하기 CTA
- 유의사항: `수량 달성 후 취소 불가 / 수량 달성 전 이탈 시 전액 환불 / 미 수령 환불 불가`

현재 백엔드 DTO로 충족되는 항목:

- 피드: 이미지, 매장명, 지역/세부지역, 상품명, 픽업 날짜, 마감 일시, 가격, 달성률, 현재 수량, 목표 수량
- 상세: 대표/상세 이미지, 매장명, 지역/세부지역, 상품명, 설명, 가격, 달성률, 현재/목표/최대 수량, 마감/픽업 일시 라벨, 위치 주소/좌표, 찜 여부, 참여 여부, 참여 가능 여부
- 체크아웃: 상품 요약, 수량, 상품 금액, 수수료, 총 결제 금액, 잔여 수량
- 결제 주문 생성: PortOne SDK 호출에 필요한 `paymentId`, `storeId`, `channelKey`, `orderName`, `amount`, `customerName`, `customerPhoneNumber`, `customerEmail`

API 수정 판단:

- 결제 테스트 플로우 진행을 막는 필드 누락은 없다.
- 피드 검색창을 실제 서버 검색으로 붙이려면 `GET /api/v1/group-buys`에 `keyword` 파라미터 추가가 필요하다. 현재는 `filter`와 `districts`만 지원한다.
- 피드에서 `36개/50개 참여중` 형태는 `currentQuantity`와 `targetQuantity`로 조합 가능하다.
- 상세의 잔여 수량은 `maxQuantity - currentQuantity`로 계산 가능하다. 서버에서 명시 필드가 필요하면 `remainingQuantity` 추가를 별도 API 개선으로 잡으면 된다.
- 상세의 `인당 최소 1개`는 현재 도메인 상 고정 정책이다. 추후 상품별 최소 주문 수량이 달라지면 별도 필드가 필요하다.
- 운영 전환 시 실제 이미지는 S3 업로드 후 DB에 S3 URL을 저장해야 한다. dev mock은 외부 placeholder URL을 사용한다.

## Dev API

Base URL:

```text
http://api.moongchijang.com/dev
```

피드:

```http
GET /api/v1/group-buys?filter=ALL&page=0&size=20
GET /api/v1/group-buys?filter=CLOSING_SOON&page=0&size=20
GET /api/v1/group-buys?filter=ALMOST_ACHIEVED&page=0&size=20
GET /api/v1/group-buys?districts=SEOUL_ALL&page=0&size=20
GET /api/v1/group-buys?districts=GYEONGGI_ALL&page=0&size=20
```

상세:

```http
GET /api/v1/group-buys/{groupBuyId}
```

체크아웃:

```http
GET /api/v1/group-buys/{groupBuyId}/checkout?quantity=1
```

결제 주문 생성:

```http
POST /api/v1/group-buys/{groupBuyId}/payment-orders
Content-Type: application/json

{
  "quantity": 1,
  "agreedNoCancelAfterGoal": true,
  "agreedRefundBeforeGoal": true,
  "agreedNoRefundAfterNoShow": true,
  "agreedNoWithdrawal": true
}
```

결제 완료 검증:

```http
POST /api/v1/payments/portone/complete
Content-Type: application/json

{
  "paymentId": "{paymentId}",
  "amount": 1000
}
```

## 테스트 groupBuyId

Seed SQL:

```text
docs/dev-payment-group-buy-seed.sql
```

DataGrip에서 dev DB에 실행하면 아래 ID가 생성된다.

```text
901001 두쫀쿠 오리지널 1개
901002 소금빵 크림치즈 6개입
901003 버터바 4종 세트
901004 잠봉뵈르 샌드위치
901005 휘낭시에 8개 박스
901006 바스크 치즈케이크
901007 생크림 도넛 6개입
901008 크루아상 샘플러
901009 수제 푸딩 4개 세트
901010 베이글 5종 세트
```

결제 테스트에는 `IN_PROGRESS`이고 잔여 수량이 있는 ID를 사용한다.

추천:

```text
901001, 901002, 901003, 901004, 901005, 901006, 901007, 901008, 901010
```

`901009`는 `ACHIEVED` 상태 확인용이다.

## 결제 호출 흐름

1. `GET /api/v1/group-buys`
2. `GET /api/v1/group-buys/{groupBuyId}`
3. 사용자가 수량 선택
4. `GET /api/v1/group-buys/{groupBuyId}/checkout?quantity={quantity}`
5. `POST /api/v1/group-buys/{groupBuyId}/payment-orders`
6. 응답의 `storeId`, `channelKey`, `paymentId`, `amount`, `orderName`, `customerName`, `customerPhoneNumber`, `customerEmail`으로 `PortOne.requestPayment` 호출
7. PortOne 성공 콜백에서 `POST /api/v1/payments/portone/complete`

주의:

- 프론트는 `PORTONE_API_SECRET`을 가지면 안 된다.
- KG이니시스 채널에서는 `customer.fullName`, `customer.phoneNumber`, `customer.email`을 누락하지 않는다. 주문 생성 응답의 `customerName`, `customerPhoneNumber`, `customerEmail`을 우선 사용한다.
- 금액은 dev seed 기준 1개당 `1000원`이다.
- 운영 웹훅 URL과 dev 웹훅 URL은 다르다. dev 검증용은 `http://api.moongchijang.com/dev/api/v1/payments/portone/webhook`이다.
