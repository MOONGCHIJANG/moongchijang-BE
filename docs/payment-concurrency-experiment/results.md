# 결제 동시성 실험 결과 정리

## 1. 문서 목적

이 문서는 로컬 환경에서 직접 돌린 결제 완료 동시성 실험 결과를 비교하기 위한 요약본이다.

- 상세 실행 절차: `docs/payment-concurrency-experiment/setup.md`
- 데이터 시드: `docs/payment-concurrency-experiment/seed.sql`
- 데이터 초기화: `docs/payment-concurrency-experiment/reset.sql`
- 로그 보관 폴더: `docs/payment-concurrency-experiment/logs`

실험을 돌릴 때는 먼저 `.experiment-logs`에 로그를 저장했고, 나중에 남길 로그만 `docs/payment-concurrency-experiment/logs`로 옮겨 두는 방식으로 정리한다. 문서에는 비교에 필요한 결과와 해석만 남긴다.

## 2. 실험 공통 조건

- 앱 인스턴스 2개: `8081`, `8082`
- 같은 MySQL, 같은 Redis 공유
- 서로 다른 구매자 N명이 같은 공구에 대해 동시에 결제 완료를 시도하도록 준비
- 실험 대상 공구:
  - `group_buy_id = 1`
  - `status = IN_PROGRESS`
  - `current_quantity = 0`에서 시작
  - `target_quantity = 50`
  - `max_quantity = 100`
- 매 실험 전 `reset.sql`로 초기화

## 3. 측정 기준

각 시나리오에서 아래 값을 동일하게 확인했다.

- `current_quantity`
- `payment_orders` 상태별 개수
- `participation` 개수
- `payments` 승인 건수

실험 후 숫자를 볼 때는 아래 관계가 맞는지 먼저 확인했다.

- `current_quantity = APPROVED = participation`

## 4. 20건 실험 결과

| 시나리오 | 요청 수 | APPROVED | READY | participation | current_quantity | 비고 |
| --- | ---: | ---: | ---: | ---: | ---: | --- |
| `all-off` | 20 | 11 | 9 | 11 | 11 | 보호 없음 |
| `distributed-lock-only` | 20 | 12 | 8 | 12 | 12 | 분산락만 사용 |
| `full-protection` | 20 | 14 | 6 | 14 | 14 | 분산락 + DB락 + 중복 처리 |
| `lock-released-before-commit` | 20 | 10 | 10 | 10 | 10 | 락이 커밋 전에 풀리도록 강제 |

### 4.1 해석

- `all-off`에서는 보호가 없을 때 승인 완료 수가 낮고, 실패 요청이 많이 남는 걸 확인했다.
- `distributed-lock-only`에서는 `all-off`보다 더 많은 요청이 끝까지 반영되는 걸 확인했다.
- `full-protection`은 20건 기준으로 가장 많은 승인 완료 수를 만들었다.
- `lock-released-before-commit`은 같은 락 기반 접근이어도 트랜잭션 경계가 잘못되면 성공 수가 눈에 띄게 줄어든다는 점을 확인하기 위해 별도로 돌린 실험이다.

## 5. 50건 실험 결과

| 시나리오 | 요청 수 | APPROVED | READY | participation | current_quantity | 비고 |
| --- | ---: | ---: | ---: | ---: | ---: | --- |
| `all-off-50` | 50 | 8 | 42 | 8 | 8 | 보호 없음 |
| `distributed-lock-only-50` | 50 | 17 | 33 | 17 | 17 | 분산락만 사용 |
| `full-protection-50` | 50 | 19 | 31 | 19 | 19 | 분산락 + DB락 + 중복 처리 |

### 5.1 해석

- `all-off-50`는 요청 수를 50건으로 올리자 승인 완료 수가 8건까지 떨어졌다.
- `distributed-lock-only-50`는 `all-off-50`보다 두 배 이상 많은 요청을 반영했다.
- `full-protection-50`는 50건 기준으로 가장 높은 승인 완료 수를 만들었다.
- 세 시나리오 모두 최종적으로 `APPROVED = participation = current_quantity`가 맞아서, 마지막 반영 상태 자체는 정합하게 유지된 걸 확인했다.

## 6. 100건 실험 결과

| 시나리오 | 요청 수 | APPROVED | READY | participation | current_quantity | 비고 |
| --- | ---: | ---: | ---: | ---: | ---: | --- |
| `all-off-100` | 100 | 21 | 79 | 21 | 21 | 재실행 후 동일 값 확인 |
| `distributed-lock-only-100` | 100 | 17 | 83 | 17 | 17 | 재실행 결과 확정 |
| `full-protection-100` | 100 | 18 | 82 | 18 | 18 | 현재 baseline 결과 |
| `full-protection-100-tuned-1` | 100 | 23 | 77 | 23 | 23 | `lockLeaseMs = 120000` |
| `full-protection-100-tuned-2` | 100 | 22 | 78 | 22 | 22 | `lockLeaseMs = 30000` |
| `full-protection-100-tuned-3` | 100 | 50 | 42 | 50 | 50 | `lockLeaseMs = 120000`, `lockWaitMs = 1500` |
| `full-protection-100-tuned-4` | 100 | 50 | 2 | 50 | 50 | `lockLeaseMs = 120000`, `lockWaitMs = 1500`, `lockRetryCount = 1`, `lockRetryDelayMs = 100` |

### 6.1 해석

- 100건 부하에서는 `all-off-100`이 `distributed-lock-only-100`, `full-protection-100`보다 높은 값으로 나왔다.
- 이 결과를 보고 "보호가 많을수록 항상 승인 수가 더 높다"라고 단순하게 적기는 어렵다고 판단했다.
- 특히 `distributed-lock-only-100`, `full-protection-100`은 고부하 구간에서 승인 수 변동폭이 있었고, 이 부분은 실패 사유 로그를 함께 보고 다시 해석해야 한다.
- 그래서 100건 결과는 먼저 baseline으로 남겨 두고, 이후 조정 실험은 별도 시나리오로 나눠 기록하는 방향으로 잡았다.
- `full-protection-100-tuned-1`, `full-protection-100-tuned-2`를 추가로 돌려 보니 둘 다 baseline `18`보다는 높은 값이 나왔다.
- 다만 `23`, `22` 수준이라 큰 폭의 개선이라기보다는, lease 값을 바꾸면 승인 완료 수가 소폭 흔들리는 정도로 보는 편이 맞겠다고 판단했다.
- 이후 `full-protection-100-tuned-3`에서 `lockWaitMs`까지 같이 조정해 보니 승인 완료 수가 `50`까지 올라갔고, 공구 상태도 `ACHIEVED`로 전이됐다.
- 이 결과를 보고 나서는 lease 시간 자체보다 "락을 얼마 동안 기다리게 둘 것인가"가 더 큰 변수일 수 있겠다고 정리했다.
- `full-protection-100-tuned-4`에서는 락 획득 실패 재시도를 한 번 더 붙여 봤는데, `READY`는 `2`까지 줄었지만 그 대신 `FAILED`가 `48`까지 늘었다.
- 그래서 재시도 자체는 "요청을 더 안쪽까지 밀어 넣는 효과"는 있었지만, 최종 승인 수를 늘리기보다는 목표 수량 도달 이후 실패를 더 많이 만들었다고 해석했다.

### 6.2 100건 튜닝 실험 메모

| 시나리오 | 변경 변수 | APPROVED | READY | 참여 반영 수 | 확인한 점 |
| --- | --- | ---: | ---: | ---: | --- |
| `full-protection-100-tuned-1` | `lockLeaseMs = 120000` | 23 | 77 | 23 | baseline보다 소폭 상승 |
| `full-protection-100-tuned-2` | `lockLeaseMs = 30000` | 22 | 78 | 22 | `tuned-1`과 큰 차이는 없음 |
| `full-protection-100-tuned-3` | `lockLeaseMs = 120000`, `lockWaitMs = 1500` | 50 | 42 | 50 | 목표 수량 달성, `ACHIEVED` 전이 확인 |
| `full-protection-100-tuned-4` | `lockLeaseMs = 120000`, `lockWaitMs = 1500`, `lockRetryCount = 1`, `lockRetryDelayMs = 100` | 50 | 2 | 50 | READY는 거의 사라졌지만 FAILED가 크게 증가 |

- 두 tuned 시나리오 모두 `APPROVED = participation = current_quantity`는 맞았다.
- 그래서 lease 조정은 "정합성 보완"보다는 "처리량 미세 조정"에 가까운 변수라고 정리했다.
- baseline `18`, tuned `22~23`을 같이 두고 보면, 지금 병목은 lease 값 하나보다 락 경쟁 자체에 더 가깝다고 해석하는 편이 자연스럽다.
- `tuned-3`까지 같이 놓고 보니, lease 값만 바꾸는 것보다 lock wait 시간을 늘려 락 획득 실패를 줄이는 쪽이 더 직접적인 개선이었다.
- 다만 `FAILED 8`, `READY 42`가 남아 있어서 여기서 끝난 건 아니고, 다음 단계에서는 실패 사유와 재시도 정책까지 같이 봐야겠다고 정리했다.
- `tuned-3` 로그를 다시 확인해 보니 `FAILED 8`은 전부 `PAYMENT_QUANTITY_EXCEEDED`였고, `READY 42`는 전부 `GROUPBUY_LOCK_ACQUISITION_FAILED`였다.
- 그래서 이 실험에서는 "실패 8건"과 "반영되지 않은 42건"의 원인을 분리해서 봐야겠다고 정리했다.
- `tuned-4` 로그를 다시 확인해 보니 `FAILED 48`은 전부 `PAYMENT_QUANTITY_EXCEEDED`, `READY 2`는 `GROUPBUY_LOCK_ACQUISITION_FAILED`였다.
- 즉 재시도로 락 획득 실패는 거의 없앴지만, 그만큼 더 많은 요청이 목표 수량 초과 검증 단계까지 들어가서 실패로 전환됐다고 정리했다.

## 7. 현재까지의 핵심 관찰

1. 보호가 없는 구성은 요청 수가 올라갈수록 빠르게 무너지는 모습을 확인했다.
2. 20건과 50건에서는 `full-protection`이 가장 많은 요청을 끝까지 반영했다.
3. `lock-released-before-commit` 실험으로, 락을 쓰더라도 커밋 전에 락이 풀리면 보호력이 약해질 수 있음을 직접 확인했다.
4. 100건 구간에서는 `full-protection`도 기대만큼 높은 승인 수를 만들지 못했고, 이 지점부터는 튜닝 실험이 필요하다고 판단했다.
5. lease 값만 조정한 `tuned-1`, `tuned-2`는 baseline보다 높은 값이 나왔지만, 개선 폭은 크지 않았다.
6. `lockWaitMs`까지 같이 조정한 `tuned-3`에서는 승인 완료 수가 `50`까지 올라가고 공구도 `ACHIEVED`로 전이됐다.
7. 지금까지 결과만 보면, 이 실험에서는 DB 락보다 먼저 분산락 대기 전략이 실제 처리량에 더 큰 영향을 줬다고 보는 편이 자연스럽다.
8. 다만 `tuned-4`처럼 락 재시도를 바로 붙이면 `READY`는 줄일 수 있어도 `FAILED`를 크게 늘릴 수 있어서, 재시도는 그대로 쓰기보다 조건이나 횟수를 더 조심해서 다뤄야겠다고 판단했다.

## 8. 로그 관리 원칙

- 문서에는 요약 결과만 남긴다.
- 실험 직후 로그는 `.experiment-logs`에 저장하고, 나중에 남길 로그만 `docs/payment-concurrency-experiment/logs`로 옮긴다.
- 각 시나리오별로 아래 3종 파일을 함께 보관하면 나중에 다시 보기 편하다.
  - `*-harness.filtered.log`
  - `*-result.log`
  - `*-audit.log`

이 방식이 좋은 이유는 다음과 같다.

- 문서는 짧게 유지할 수 있다.
- 필요할 때만 근거 로그를 바로 열어볼 수 있다.
- 이후 튜닝 실험 결과도 같은 규칙으로 누적하기 쉽다.

## 9. 다음 단계 메모

- `full-protection-100-tuned-3`의 실패 사유를 먼저 다시 확인한다.
- 실패 사유가 여전히 락 획득 실패 위주라면, 다음 단계는 lease 조정보다 락 획득 전략이나 재시도 정책을 보는 편이 맞는지 확인한다.
- 기존 baseline 결과는 유지하고, 이후 실험도 `full-protection-100-tuned-*` 같은 새 이름으로 기록한다.
- 다음 조정 실험에서는 재시도를 그대로 늘리기보다, `lockWaitMs`, `lockRetryCount`, `lockRetryDelayMs` 조합을 더 보수적으로 다시 잡아 본다.
