# MCJ 부하테스트 결과 기록 템플릿

## 1. 기본 정보

- 테스트 일시:
- 실행자:
- 대상 브랜치:
- 대상 커밋:
- 대상 환경:
- 대상 URL:

## 2. 시나리오 정보

- 시나리오명:
- 대상 흐름:
- 목적:
- 사전 데이터 조건:
- 인증 정보 사용 여부:
- 외부 연동 포함 여부:

## 3. 실행 조건

- 실행 도구:
- 실행 명령어:
- VU / stages:
- 지속 시간:
- 테스트 데이터:
- 결과 파일 경로:
- 쓰기성 요청 포함 여부:
- 상태 변경 허용 여부:

## 4. 주요 결과

- 총 요청 수:
- 실패율:
- 평균 응답시간:
- p95:
- p99:
- HTTP 4xx 비율:
- HTTP 5xx 비율:
- timeout 발생 여부:

## 5. 모니터링 확인

- Grafana 확인 패널:
- Prometheus 확인 쿼리:
- 애플리케이션 로그 확인 내용:
- 인프라 병목 징후:

### 5.1 결제 시나리오 추가 확인 항목

- 확인 대시보드:
- 확인 대상 flow tag:
- 결제 주문 생성 지표 변화:
- 결제 승인 지표 변화:
- 웹훅 처리 지표 변화:
- 환불/취소 지표 변화:
- PortOne API 성공/실패 지표 변화:
- PortOne API latency p95 변화:
- HTTP 4xx / 5xx 해석:

## 6. 결과 해석

- 병목 의심 지점:
- 특이사항:
- 재현 필요 여부:

## 7. 후속 액션

- 개선 필요사항:
- 추가 테스트 필요사항:
- 관련 이슈/PR:

## 8. 결제 시나리오 기록 예시

### 8.1 결제 모니터링 실패 흐름 예시

- 시나리오명: `payment-monitoring`
- 대상 흐름: 존재하지 않는 주문 기준 결제 완료 실패 응답 확인
- 목적: 운영 데이터 변경 없이 결제 실패 지표 및 응답속도 노출 여부 확인
- 쓰기성 요청 포함 여부: 아니오
- 상태 변경 허용 여부: 아니오
- 확인 대시보드: `MCJ Dev Payment Monitoring`
- 확인 대상 flow tag: `payment_flow=complete_failure`

### 8.2 결제 주문 생성 포함 흐름 예시

- 시나리오명: `payment-monitoring`
- 대상 흐름: 결제 주문 생성 + 결제 실패 응답 확인
- 목적: 주문 생성 도메인 metric 및 결제 실패 metric 동시 확인
- 쓰기성 요청 포함 여부: 예
- 상태 변경 허용 여부: `ALLOW_STATE_CHANGE=true`
- 확인 대시보드: `MCJ Dev Payment Monitoring`
- 확인 대상 flow tag: `payment_flow=create_order`, `payment_flow=complete_failure`

## 9. 첨부 권장 항목

- `k6` summary export JSON 경로
- Grafana 패널 스크린샷 또는 수치 요약
- Prometheus 쿼리 결과
- 애플리케이션 에러 로그 발췌
- 실행 시 사용한 환경변수 목록
