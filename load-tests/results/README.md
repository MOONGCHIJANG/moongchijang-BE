# Load Test Results

이 디렉터리는 부하테스트 실행 결과를 저장합니다.

## 저장 규칙

- `k6` summary export 결과는 JSON으로 저장합니다.
- 실행 결과 해석 및 모니터링 확인 내용은 Markdown으로 저장합니다.
- 파일명은 `YYYY-MM-DD-<scenario-name>` 형식을 사용합니다.

예시:

- `2026-06-13-group-buy-read-summary.json`
- `2026-06-13-group-buy-read.md`

## 기록 기준

- Markdown 결과 정리는 `docs/load-test-result-template.md`를 기준으로 작성합니다.
- 시나리오별 결과는 재실행 시 덮어쓰지 않고 날짜 또는 회차를 구분해 저장합니다.
- PR 또는 이슈에 공유할 때는 결과 요약 Markdown을 기준으로 사용합니다.
