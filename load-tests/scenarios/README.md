# Scenarios

이 디렉터리에는 실제 부하테스트 시나리오를 추가합니다.

우선순위는 아래 순서를 따릅니다.

1. 공동구매 목록/상세 조회
2. 결제 주문 생성/결제 승인
3. 결제 취소/웹훅 처리
4. 마이페이지/운영 조회
5. 환불/배치성 흐름

신규 시나리오는 `../templates/scenario-template.js`를 기준으로 작성합니다.

현재 추가된 시나리오:

- `group-buy-read.js`: 공동구매 목록 → 상세 → 단건 진행률 → 다건 진행률 조회 흐름 검증
- `mypage-read.js`: 탭 건수 → 상태별 참여 내역 → 진행 중/픽업 대기 탭 → 환불/개설 요청 조회 흐름 검증
- `admin-read.js`: 운영 요약 → 미확정 발주 → 긴급 환불 → 정산 대시보드/목록 → 환불 요청 목록 조회 흐름 검증
- `favorite-stateful-read.js`: 찜 목록 조회 → 공구 진행률 조회 → 조회자 heartbeat 갱신 흐름 검증
- `payment-monitoring.js`: 안전한 기본 실패 흐름 중심의 결제 모니터링 smoke 시나리오

## 실행 메모

- 조회성 시나리오는 기본적으로 읽기 전용 흐름을 사용합니다.
- `favorite-stateful-read.js` 는 heartbeat 갱신이 포함되므로 실행 전 대상 데이터 영향 여부를 확인합니다.
- `payment-monitoring.js` 는 실패 흐름을 기본값으로 사용하며, 실제 주문 생성은 `RUN_CREATE_ORDER=true` 와 `ALLOW_STATE_CHANGE=true` 를 함께 지정한 경우에만 수행합니다.
