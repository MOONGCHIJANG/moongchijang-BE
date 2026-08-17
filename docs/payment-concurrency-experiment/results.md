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
| `full-protection-100-tuned-4` | 100 | 50 | 5 | 50 | 50 | `lockWaitMs = 1500`, `lockLeaseMs = 120000`, `lockRetryCount = 1`, `lockRetryDelayMs = 100` |
| `full-protection-100-tuned-5` | 100 | 50 | 0 | 50 | 50 | `lockWaitMs = 2000`, `lockLeaseMs = 120000`, `lockRetryCount = 2`, `lockRetryDelayMs = 150` |
| `full-protection-100-post-fix-1` | 100 | 73 | 27 | 73 | 73 | `ACHIEVED` 상태도 수량 증가 허용 후 재실험 |
| `full-protection-100-post-fix-2` | 100 | 100 | 0 | 100 | 100 | 쿼리 수정 반영 후 최종 재실험 |

### 6.1 해석

- 100건 부하에서는 `all-off-100`이 `distributed-lock-only-100`, `full-protection-100`보다 높은 값으로 나왔다.
- 이 결과만 놓고 보면 "보호가 많을수록 항상 승인 수가 더 높다"라고 단순하게 적기는 어렵다.
- 특히 `distributed-lock-only-100`, `full-protection-100`은 고부하 구간에서 승인 수 변동폭이 있었고, 이 부분은 실패 사유 로그를 함께 보고 다시 해석해야 한다.
- 그래서 100건 결과는 먼저 baseline으로 남겨 두고, 이후 조정 실험은 별도 시나리오로 나눠 기록하는 방향으로 잡았다.
- `full-protection-100-tuned-1`, `full-protection-100-tuned-2`를 추가로 돌려 보니 둘 다 baseline `18`보다는 높은 값이 나왔다.
- 다만 `23`, `22` 수준이라 큰 폭의 개선이라기보다는, lease 값을 바꾸면 승인 완료 수가 소폭 흔들리는 정도로 보는 편이 더 자연스럽다.
- 이후 `full-protection-100-tuned-3`에서 `lockWaitMs`까지 같이 조정해 보니 승인 완료 수가 `50`까지 올라갔고, 공구 상태도 `ACHIEVED`로 전이됐다.
- 이 결과를 보면 lease 시간 자체보다 "락을 얼마 동안 기다리게 둘 것인가"가 더 큰 변수일 수 있다고 볼 수 있다.
- 그 다음 `tuned-4`, `tuned-5`에서는 재시도까지 추가해 봤는데, 둘 다 `APPROVED 50`을 유지한 채 `READY`를 더 줄이는 방향으로 움직였다.
- 그래서 이 구간부터는 "성공 수를 더 늘린다"기보다, "남은 요청을 READY에 두지 않고 더 명시적인 최종 상태로 수렴시킨다"는 관점으로 보는 편이 자연스럽다.
- 그런데 `tuned-3`부터 `tuned-5`까지 숫자를 다시 놓고 보니, `maxQuantity = 100`이 남아 있는데도 성공 수가 50에서 멈춘 점이 계속 이상했다.
- 그래서 로그만 보는 걸로 끝내지 않고 조건부 증가 쿼리까지 다시 확인했고, 그 결과 `ACHIEVED` 상태에서는 수량 증가가 막혀 있다는 걸 찾았다.
- 이 부분을 고친 뒤 다시 돌린 `post-fix-1`에서는 승인 수가 `73`까지 올라갔고, 마지막 `post-fix-2`에서는 `100 / 100`까지 모두 반영되는 걸 확인했다.

### 6.2 100건 튜닝 실험 메모

| 시나리오 | 변경 변수 | APPROVED | READY | 참여 반영 수 | 확인한 점 |
| --- | --- | ---: | ---: | ---: | --- |
| `full-protection-100-tuned-1` | `lockLeaseMs = 120000` | 23 | 77 | 23 | baseline보다 소폭 상승 |
| `full-protection-100-tuned-2` | `lockLeaseMs = 30000` | 22 | 78 | 22 | `tuned-1`과 큰 차이는 없음 |
| `full-protection-100-tuned-3` | `lockLeaseMs = 120000`, `lockWaitMs = 1500` | 50 | 42 | 50 | 목표 수량 달성, `ACHIEVED` 전이 확인 |
| `full-protection-100-tuned-4` | `lockWaitMs = 1500`, `lockLeaseMs = 120000`, `lockRetryCount = 1`, `lockRetryDelayMs = 100` | 50 | 5 | 50 | 목표 수량 유지, READY 대폭 감소 |
| `full-protection-100-tuned-5` | `lockWaitMs = 2000`, `lockLeaseMs = 120000`, `lockRetryCount = 2`, `lockRetryDelayMs = 150` | 50 | 0 | 50 | 모든 요청이 최종 상태로 수렴 |

- 두 tuned 시나리오 모두 `APPROVED = participation = current_quantity`는 맞았다.
- 그래서 lease 조정은 "정합성 보완"보다는 "처리량 미세 조정"에 가까운 변수로 볼 수 있다.
- baseline `18`, tuned `22~23`을 같이 두고 보면, 지금 병목은 lease 값 하나보다 락 경쟁 자체에 더 가깝다고 해석하는 편이 자연스럽다.
- `tuned-3`까지 같이 놓고 보니, lease 값만 바꾸는 것보다 lock wait 시간을 늘려 락 획득 실패를 줄이는 쪽이 더 직접적인 개선이었다.
- 다만 `FAILED 8`, `READY 42`가 남아 있어서, 이 단계에서는 실패 사유와 재시도 정책을 함께 봐야 한다는 점이 드러났다.
- `tuned-3` 로그를 다시 확인해 보니 `FAILED 8`은 전부 `PAYMENT_QUANTITY_EXCEEDED`였고, `READY 42`는 전부 `GROUPBUY_LOCK_ACQUISITION_FAILED`였다.
- 그래서 이 실험에서는 "실패 8건"과 "반영되지 않은 42건"의 원인을 분리해서 볼 필요가 있다.
- `tuned-4` 로그를 다시 보니 `SUCCESS 50`, `PAYMENT_QUANTITY_EXCEEDED 45`, `GROUPBUY_LOCK_ACQUISITION_FAILED 5`였다.
- 이 결과를 보면 재시도 1회만으로도 락 획득 실패를 많이 줄일 수 있었다. `tuned-3`에서는 READY가 42건이었는데, `tuned-4`에서는 5건까지 줄었다.
- `tuned-5` 로그에서는 `SUCCESS 50`, `PAYMENT_QUANTITY_EXCEEDED 50`만 남았고 `GROUPBUY_LOCK_ACQUISITION_FAILED`는 사라졌다.
- 여기서 눈에 띄는 점은, 성공 수가 50에서 더 늘어나지는 않았지만 모든 요청이 `성공 또는 명시적 실패`로 끝났다는 점이었다. 그래서 이 실험은 "처리량을 늘린다"보다 "중간 상태를 줄이고 최종 결과를 빨리 확정한다"는 방향의 튜닝으로 볼 수 있다.
- 다만 여기서 `PAYMENT_QUANTITY_EXCEEDED`를 그대로 "maxQuantity 100을 초과했다"로 읽으면 코드 의미와 어긋난다. 실제로는 `currentQuantity = 50`, `maxQuantity = 100`인 상태에서 실패했기 때문이다.
- 코드까지 확인해 보니 원인은 `increaseCurrentQuantityIfAvailable(...)` 조건부 증가 쿼리가 `GroupBuyStatus.IN_PROGRESS`에서만 동작하도록 되어 있었기 때문이다.
- 즉 `currentQuantity`가 `targetQuantity = 50`에 도달하는 순간 공구 상태가 `ACHIEVED`로 바뀌고, 그 뒤 요청들은 수량이 남아 있어도 조건부 update가 0건 갱신으로 끝난다.
- 서비스에서는 이 0건 갱신을 그대로 `PAYMENT_QUANTITY_EXCEEDED`로 번역하고 있어서, 실험 로그만 보면 "정원이 다 차서 실패했다"처럼 보이지만 실제로는 "목표 수량 달성 후 상태가 바뀌어 더 이상 증가되지 않았다"에 더 가깝다.
- 그래서 `tuned-4`, `tuned-5`의 실패는 `maxQuantity = 100`을 다 써서 난 실패라기보다, `targetQuantity = 50` 달성 후 `ACHIEVED` 상태로 전이된 뒤에도 조건부 증가 쿼리가 `IN_PROGRESS`만 허용하고 있었기 때문에 생긴 실패로 해석하는 편이 맞다.

### 6.3 post-fix 재실험 메모

| 시나리오 | 변경 내용 | APPROVED | READY | 참여 반영 수 | 확인한 점 |
| --- | --- | ---: | ---: | ---: | --- |
| `full-protection-100-post-fix-1` | 조건부 증가 쿼리에 `ACHIEVED` 허용 | 73 | 27 | 73 | 50에서 막히던 현상이 풀리고 승인 수가 더 올라감 |
| `full-protection-100-post-fix-2` | 같은 코드 기준으로 설정 다시 맞춰 재실험 | 100 | 0 | 100 | `current_quantity = 100`, `APPROVED = 100`까지 도달 |

- `post-fix-1`은 쿼리 수정이 실제로 의미가 있었는지 확인하려고 바로 다시 돌린 실험이다.
- 이때는 `APPROVED 73`, `READY 27`, `participation 73`, `current_quantity 73`이 나왔고, 공구 상태도 `ACHIEVED`를 유지한 채 50을 넘어 더 올라갔다.
- 그래서 `tuned-3`~`tuned-5`에서 보이던 `PAYMENT_QUANTITY_EXCEEDED`의 핵심 원인은 "정말로 수량이 다 찼다"가 아니라, `ACHIEVED` 상태 전이 이후에도 증가 쿼리가 계속 동작하지 않았던 쪽에 더 가까웠다고 해석했다.
- `post-fix-1` 정제 로그를 다시 보니 남은 27건은 주로 `GROUPBUY_LOCK_ACQUISITION_FAILED`로 남아 있었다. 즉 상태 조건 문제를 풀고 나니, 그다음 병목은 다시 락 획득 경쟁 쪽으로 돌아왔다.
- 마지막 `post-fix-2`는 같은 수정 상태에서 설정을 한 번 더 맞춰 돌린 실험이다.
- 결과는 `APPROVED 100`, `participation 100`, `current_quantity 100`이었고, 공구 상태도 최종적으로 `COMPLETED`까지 전이됐다.
- 여기까지 확인하고 나면, 이번 로컬 실험 기준으로는 "왜 50에서 멈췄는지"와 "그걸 고친 뒤 100까지 갈 수 있는지"를 둘 다 설명할 수 있다.

## 7. 현재까지의 핵심 관찰

1. 보호가 없는 구성은 요청 수가 올라갈수록 빠르게 무너지는 모습을 확인했다.
2. 20건과 50건에서는 `full-protection`이 가장 많은 요청을 끝까지 반영했다.
3. `lock-released-before-commit` 실험으로, 락을 쓰더라도 커밋 전에 락이 풀리면 보호력이 약해질 수 있음을 직접 확인했다.
4. 100건 구간에서는 `full-protection`도 기대만큼 높은 승인 수를 만들지 못했고, 이 지점부터는 튜닝 실험이 필요했다.
5. lease 값만 조정한 `tuned-1`, `tuned-2`는 baseline보다 높은 값이 나왔지만, 개선 폭은 크지 않았다.
6. `lockWaitMs`까지 같이 조정한 `tuned-3`에서는 승인 완료 수가 `50`까지 올라가고 공구도 `ACHIEVED`로 전이됐다.
7. 지금까지 결과만 보면, 이 실험에서는 DB 락보다 먼저 분산락 대기 전략이 실제 처리량에 더 큰 영향을 줬다고 보는 편이 자연스럽다.
8. `tuned-4`, `tuned-5`를 통해 재시도는 성공 수를 무한히 늘리는 수단이라기보다, 락 획득 실패를 줄이고 요청을 더 명확한 최종 상태로 수렴시키는 수단이라는 점을 확인했다.
9. 이번 코드 기준으로는 `targetQuantity = 50` 달성 후 `ACHIEVED` 상태로 바뀌면, `maxQuantity = 100`이 남아 있어도 조건부 수량 증가 쿼리가 더 이상 동작하지 않았다.
10. 그래서 처음에는 "50을 넘기면 안 되는 설계인가?"라고 봤지만, 쿼리를 수정한 뒤 `post-fix-1`에서 `73`, `post-fix-2`에서 `100`까지 올라가는 걸 확인하면서 원인이 명확해졌다.
11. 최종적으로는 "락 대기 전략"과 "상태 전이 이후에도 수량 증가가 가능한 조건"이 둘 다 맞아야 100건 부하에서 끝까지 반영된다는 걸 확인했다.

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

## 9. 이번 실험에서 남긴 결론

- 20건, 50건 구간에서는 보호 계층이 있는 시나리오가 더 안정적으로 요청을 끝까지 반영했다.
- 100건 구간에서는 단순히 락을 더 많이 건다고 해결되지 않았고, 어떤 실패가 락 경쟁 때문인지, 어떤 실패가 상태 조건 때문인지 분리해서 봐야 했다.
- `tuned-3`~`tuned-5`는 "왜 50에서 멈추는가"를 찾는 과정이었다.
- `post-fix-1`, `post-fix-2`는 그 원인을 실제로 고친 뒤 결과가 달라지는지 확인한 과정이었다.
- 최종적으로는 `current_quantity = 100`, `APPROVED = 100`, `participation = 100`까지 맞춰 보면서, 이번 수정이 실제로 무엇을 막고 있었는지 로컬 실험으로 설명할 수 있게 됐다.
