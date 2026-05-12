# 공구 검색 품질 관측성

이 문서는 공구 검색(`SearchOrchestrator` + `RetrievalPipeline`)의 운영 관측 기준을 정의한다.
관측 데이터는 모두 `SearchObservabilityLogger`가 INFO 레벨로 남기는 `search_retrieval` 한 줄 로그를 기준으로 한다.

대상 독자는 검색 운영자, SRE, 검색 도메인 개발자다.

## 1. 관측 진입점

| 항목 | 값 |
|------|----|
| 로거 | `com.moongchijang.domain.search.application.SearchObservabilityLogger` |
| 로그 prefix | `search_retrieval` |
| 토글 키 | `search.observability.enabled` (env: `SEARCH_OBSERVABILITY_ENABLED`, 기본 `true`) |
| 호출 위치 | `RetrievalPipeline.retrieve(...)` 1회/요청 |

로그가 비활성화되어 있으면 본 문서의 모든 지표는 산출 불가능하다. 운영 환경에서는 `true` 유지를 기본으로 한다.

## 2. `search_retrieval` 로그 필드

`SearchObservabilityLogger.logRetrieval`이 출력하는 필드 정의다. (소스: `SearchObservabilityLogger.kt`)

| 필드 | 타입 | 설명 |
|------|------|------|
| `queryHash` | string | 원본 query를 SHA-256 → 앞 16자리 hex. raw query는 절대 로그에 남기지 않는다. |
| `queryLength` | int | 원본 query의 문자 길이. 비정상 길이 탐지용. |
| `searchCase` | enum | `BOTH_DETECTED` / `PRODUCT_ONLY` / `NEIGHBORHOOD_ONLY` / `NONE_DETECTED` |
| `hasRegion` | bool | intent에서 region 추출 여부 |
| `hasProduct` | bool | intent에서 product 추출 여부 |
| `confidenceBucket` | enum | `HIGH` (≥0.9) / `MEDIUM` (≥0.65) / `LOW` (>0) / `NONE` (=0) |
| `mysqlResultCount` | int | `groupBuyRepository.searchByIntent`로 받은 정확 매칭 수 |
| `qdrantCandidateCount` | int | 벡터 후보 수 (가드 적용 전) |
| `vectorPromotedCount` | int | 가드 통과 후 promote된 벡터 후보 수 |
| `guardRejectedCount` | int | 가드가 차단한 벡터 후보 수 |
| `finalResultCount` | int | 사용자에게 반환되는 최종 결과 수 |
| `vectorOnlyResultCount` | int | matchedBy가 `{"VECTOR"}` 단일인 결과 수 |
| `topCandidateScore` | double? | 가드 결정 대상이 된 벡터 후보의 최대 score |
| `providerSources` | map<string,int> | 최종 결과의 matchedBy 분포 (`MYSQL`, `VECTOR` 등) |
| `guardRejectionReasons` | map<string,int> | 가드 거절 사유별 카운트 |

### 2.1 가드 거절 사유

`VectorCandidateRejectionReason` (소스: `VectorCandidatePromotionGuard.kt`)

| reason | 의미 |
|--------|------|
| `NO_KNOWN_TOKEN` | intent에 region/product가 전혀 없음. 벡터 단독 통과 차단. |
| `LOW_CONFIDENCE` | intent confidence가 `search.guard.min-confidence` 미만 |
| `METADATA_MISMATCH` | intent의 region/product가 매장 메타데이터와 불일치 |
| `LOW_SCORE` | NONE_DETECTED 보조 통과 임계치(`supportive-score-threshold`) 미달 |
| `UNAVAILABLE_METADATA` | `findAllById` 결과에 해당 groupBuyId 없음 (인덱스 불일치) |

## 3. raw query 미노출 정책

검색 query는 PII에 준하는 사용자 입력으로 취급한다.

- raw query는 어떤 로그/모니터링/대시보드에도 남기지 않는다.
- 식별이 필요한 경우 `queryHash` (SHA-256 앞 16자리)로만 추적한다.
- 같은 query → 같은 hash. 다른 query → 동일 hash 확률 무시 가능.
- 운영 중 raw query가 필요한 디버깅이 발생하면 개별 사용자 동의/요청 기반 절차로 한정한다. 상시 수집은 금지.

`queryLength`는 raw query와 무관하므로 분포 모니터링에 사용해도 된다.

## 4. 핵심 운영 지표

### 4.1 Zero-result rate

`finalResultCount == 0` 비율. 검색 무응답 빈도.

- 정의: `count(finalResultCount=0) / count(total)` (분 또는 일 단위 윈도우)
- 의미: 사용자 질의 중 결과 0건 비율. 높을수록 검색 효용 저하.
- 경보 권장치: 일 단위 zero-result rate가 직전 7일 평균 대비 +20%p 이상 증가하면 조사.
- 단, NONE_DETECTED + garbage query는 정상적으로 zero-result가 된다. searchCase별 분리해서 본다.

### 4.2 Guard rejection rate

`guardRejectedCount / qdrantCandidateCount` 비율.

- 의미: 벡터 후보 중 가드가 차단한 비율.
- searchCase별/reason별로 쪼개서 본다.
- 정상 패턴:
  - NONE_DETECTED → `NO_KNOWN_TOKEN`이 다수면 정상 (false positive 방어 성공)
  - PRODUCT_ONLY/BOTH_DETECTED → 가드 차단이 많으면 후보 품질 의심
- 가드 차단율이 갑자기 0이 되면 가드 비활성화 사고를 의심한다.

### 4.3 Provider contribution ratio

`providerSources`의 분포. 최종 결과가 MySQL / VECTOR / 둘 다 매치인지 비율.

- 정의: 결과 카드 1건당 matchedBy 집합을 집계.
- 의미:
  - `MYSQL` 단독 비중이 100%면 벡터가 사실상 기여 안 함 → 벡터 인덱스/임베딩 점검.
  - `VECTOR` 단독 비중이 급증하면 가드 약화 또는 정확 매칭 누락을 의심.
- 운영 초기 기대: alias/오타 케이스를 제외하면 대부분 `MYSQL` 또는 `MYSQL+VECTOR` 동시 매치.

### 4.4 Vector-only result rate

`vectorOnlyResultCount / finalResultCount`.

- 의미: 결과 중 매장 메타에는 안 잡혔는데 벡터로만 잡힌 비율.
- 이 지표가 높을수록 reranker가 벡터 후보를 그대로 노출하는 셈이라 false positive 위험 증가.
- 운영 임계: 일 단위 vector-only rate 30% 초과 지속 시 가드 임계치(`min-confidence`, `supportive-score-threshold`) 재검토.

### 4.5 Top candidate score distribution

`topCandidateScore` 히스토그램.

- 의미: 가드 결정 대상이 된 벡터 후보 중 최고 score 분포.
- 임베딩 모델/Qdrant 컬렉션이 바뀌면 분포가 함께 바뀐다. 인덱스 버전(`search.index.default-version`) 변경 시 baseline을 다시 잡는다.
- score가 전반적으로 `search.retrieval.vector-min-score` 근처에만 분포하면 후보 품질이 낮다는 신호.

## 5. PR4 평가 결과 (golden case)

검증 자산은 develop에 머지 완료된 상태다 (PR #79, MCJ-1422).

### 5.1 자산 위치

| 항목 | 경로 |
|------|------|
| 골든 케이스 CSV | `src/test/resources/search/golden-search-cases.csv` (총 17건) |
| CSV 로더 | `src/test/kotlin/com/moongchijang/support/search/GoldenSearchCaseLoader.kt` |
| 평가 메트릭 | `src/main/kotlin/com/moongchijang/domain/search/evaluation/SearchEvaluationMetrics.kt` |
| 평가 서비스 | `src/main/kotlin/com/moongchijang/domain/search/evaluation/SearchEvaluationService.kt` |
| 프로바이더 비교 러너 | `src/main/kotlin/com/moongchijang/domain/search/evaluation/SearchProviderComparison.kt` |
| 가드 통합 테스트 | `src/test/kotlin/com/moongchijang/domain/search/application/RetrievalPipelineGuardIntegrationTest.kt` |

### 5.2 PR4가 입증한 가드 효용

`RetrievalPipelineGuardIntegrationTest`가 동일 입력(`asdfqwer`, NONE_DETECTED)에 대해 가드 ON/OFF를 비교한다.

| 조건 | 입력 | 벡터 후보 | 최종 결과 |
|------|------|-----------|-----------|
| Guarded Hybrid | `asdfqwer` (NONE_DETECTED) | groupBuyId=1, score=0.95 | 0건 (`NO_KNOWN_TOKEN`으로 차단) |
| Unguarded Hybrid | 동일 | 동일 | 1건 (false positive 통과) |

`METADATA_MISMATCH` 케이스에서도 동일 패턴이 입증된다.
- 가드 ON: intent product "두쫀쿠" + 매장 productName "소금빵" 불일치 → 0건
- 즉, NONE_DETECTED / METADATA_MISMATCH 시나리오에서 가드가 false positive를 차단함을 통합 테스트로 보장한다.

### 5.3 골든 케이스에 대한 정량 결과 표기 원칙

- 본 문서는 운영 기준이므로, 실제 evaluation 수치는 평가 실행 시점에 산출하여 별도 dashboard/리포트로 관리한다.
- 본 문서 작성 시점 기준 정량 수치(Recall@10, FP 등)는 평가 harness 실행 결과에 의존하므로, 운영 환경 적용 직전에 다음을 실행/기록한다.
  - `SearchProviderComparisonRunner`로 `mysql-only`, `qdrant-only`, `hybrid (guard-on)`, `hybrid (guard-off)` 4개 프로바이더 비교
  - golden CSV의 17개 케이스 기준 `recallAtK` / `precisionAtK` / `zeroResultRate` 출력
- 합격 기준은 본 문서 §6 참조.

## 6. 운영 합격 기준 (Quality Gate)

운영 배포 전 다음을 확인한다.

| 조건 | 기준 | 출처 |
|------|------|------|
| Hybrid (guard ON) Recall@10 | ≥ 0.95 (golden 17건 기준) | evaluation harness |
| Hybrid (guard ON) false positive | 0건 (NONE_DETECTED / unknown 케이스) | evaluation harness + `RetrievalPipelineGuardIntegrationTest` |
| Unguarded Hybrid false positive | 1건 이상 재현 | 가드 효용 입증 |
| `search.observability.enabled` | `true` | prod yml |
| `search.guard.enabled` | `true` | prod yml |

기준 미달 시 §7 롤백 조건과 함께 검토한다.

## 7. 알람/롤백 트리거 후보

본 문서는 임계치 후보만 제시하며, 실제 모니터링 도구 연동은 별도 작업이다.

| 트리거 | 임계 | 권장 액션 |
|--------|------|-----------|
| zero-result rate spike | 직전 7일 평균 +20%p | 인덱스 동기화/임베딩 버전 확인 |
| vector-only rate 급증 | 일 단위 >30% 지속 | 가드 임계치 재점검 |
| guard rejection rate = 0 (PRODUCT_ONLY/BOTH_DETECTED에서) | 24h 지속 | 가드 비활성 사고 의심 |
| Qdrant 실패율 | RetrievalPipeline 경고 로그 > 1%/min | 런북 §Qdrant fallback 참조 |
| Gemini 실패율 | 키워드 추출 실패 로그 > 5%/min | 런북 §Gemini fallback 참조 |

## 8. 관련 코드 진입점

| 관심사 | 파일 |
|--------|------|
| 검색 진입점 | `domain/search/application/SearchOrchestrator.kt` |
| 검색 파이프라인 | `domain/search/application/RetrievalPipeline.kt` |
| 가드 | `domain/search/application/VectorCandidatePromotionGuard.kt` |
| 관측 로거 | `domain/search/application/SearchObservabilityLogger.kt` |
| 설정 | `global/config/SearchProperties.kt`, `global/config/QdrantProperties.kt` |
