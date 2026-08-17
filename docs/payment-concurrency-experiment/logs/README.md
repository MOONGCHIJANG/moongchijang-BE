# 로그 보관 가이드

이 폴더는 결제 동시성 실험에서 추출한 정제 로그를 보관하기 위한 위치다.

권장 보관 파일:

- `*-harness.filtered.log`
- `*-result.log`
- `*-audit.log`

예시:

- `full-protection-run2-harness.filtered.log`
- `full-protection-run2-result.log`
- `full-protection-run2-audit.log`

원본 로그는 로컬에서 먼저 별도 폴더에 저장해 확인하고, 비교에 필요한 정제 로그만 이 폴더에 옮겨 남겼다.

커밋 전에는 로그 라인 앞에 붙은 로컬 경로 접두어를 제거한다.

- 예: `.experiment-logs/full-protection-100.log:2026-...`
- 정리 후: `2026-...`

권장 방식은 "원본 전체 로그"를 그대로 공개하는 것이 아니라, 실험 비교에 필요한 정제 로그만 선별해서 남기는 것이다.

- `harness.filtered.log`: 요청 수, 성공/실패 수, 상태 요약
- `result.log`: 대표 성공/실패 응답
- `audit.log`: 상태 전이 흐름 확인

민감할 수 있는 내용이 섞일 수 있으므로, 원본 전체 로그는 로컬 확인용으로만 두고 정제 로그만 커밋 대상으로 관리한다.

예시 명령:

```bash
mv .experiment-logs/full-protection-run2-harness.filtered.log \
  docs/payment-concurrency-experiment/logs/
```

```bash
mv .experiment-logs/full-protection-run2-result.log \
  docs/payment-concurrency-experiment/logs/
```

```bash
mv .experiment-logs/full-protection-run2-audit.log \
  docs/payment-concurrency-experiment/logs/
```
