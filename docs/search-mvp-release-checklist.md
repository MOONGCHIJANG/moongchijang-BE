# 공구 검색 릴리스 체크리스트

이 문서는 공구 검색을 운영에 배포/롤백할 때 따라가는 체크리스트다.
관측 정의는 `docs/search-quality-observability.md`, 장애 대응은 `docs/search-mvp-runbook.md`를 참조한다.

대상 독자: 검색 도메인 PR 작성자, 릴리스 책임자.

## 1. 적용 범위

다음 변경에 본 체크리스트를 적용한다.

- 검색 도메인 로직 변경 (`domain/search/**`)
- `search.*` 설정 키 값 변경
- `qdrant.*` 설정 키 값 변경
- `gemini.api-key` 변경
- 인덱스 재빌드/컬렉션 swap
- 임베딩 모델 / 벡터 차원 변경
- 가드 임계치 (`min-confidence`, `supportive-score-threshold`) 변경

문서 단독 변경에는 적용하지 않는다.

## 2. Pre-rollout 체크리스트

### 2.1 코드/스펙

- [ ] PR이 단일 SCRUM 티켓에 매핑되며 제목이 `[MCJ-XXXX] type: ...` 형식이다.
- [ ] 변경 범위가 PR description의 "작업한 내용"에 빠짐없이 적혀 있다.
- [ ] 비즈니스 로직 변경 시 `features/groupbuy/` 또는 검색 관련 스펙 문서가 함께 갱신되었다.
- [ ] 검색 ranking weight를 변경했다면, 그 근거(데이터/실험)가 PR 본문에 적혀 있다.

### 2.2 테스트

- [ ] `./gradlew classes testClasses` 통과.
- [ ] `./gradlew test --tests "com.moongchijang.domain.search.*"` 통과.
- [ ] `RetrievalPipelineGuardIntegrationTest`가 통과 (가드 ON/OFF 비교 시나리오).
- [ ] `SearchOrchestratorIntegrationTest`가 통과 (Case 1~4 + 폴백).
- [ ] 신규 케이스/회귀 케이스가 있으면 `src/test/resources/search/golden-search-cases.csv`에 추가되었다.
- [ ] (선택) Qdrant 환경이 있다면 `QdrantCrudIntegrationTest` 실행 결과 첨부.
- [ ] (선택) Gemini 키가 있다면 `RealGeminiKeywordExtractionTest` 실행 결과 첨부.

### 2.3 평가 (evaluation harness)

검색 품질에 영향을 주는 변경에는 다음을 PR에 첨부한다.

- [ ] `SearchProviderComparisonRunner` 실행 결과:
  - `mysql-only` / `qdrant-only` / `hybrid (guard-on)` / `hybrid (guard-off)` 4개 프로바이더 비교
  - golden CSV의 17개 케이스 기준 `recallAtK`, `precisionAtK`, `zeroResultRate`
- [ ] Quality Gate (observability §6) 통과:
  - Hybrid (guard ON) Recall@10 ≥ 0.95
  - Hybrid (guard ON) NONE_DETECTED / unknown 케이스 false positive = 0
  - Unguarded Hybrid false positive 1건 이상 재현되어 가드 효용이 입증됨
- [ ] 임베딩 모델 / 벡터 차원 변경 시 `topCandidateScore` 분포 baseline을 다시 측정해서 첨부.

### 2.4 운영 설정 / 인프라

- [ ] 신규/변경 env가 있으면 운영 secret 매니저에 등록되었다 (값은 PR/문서에 적지 않는다).
- [ ] `SEARCH_GUARD_ENABLED=true` 유지.
- [ ] `SEARCH_OBSERVABILITY_ENABLED=true` 유지.
- [ ] `QDRANT_ENABLED`, `QDRANT_URL`, `QDRANT_COLLECTION_NAME`이 의도한 값과 일치.
- [ ] 인덱스 재구축이 동반되면 신규 컬렉션이 사전에 생성/적재 완료.
- [ ] 롤백 전용 환경 스위치(이전 컬렉션명, 이전 가드 임계치 등)가 한 번에 되돌릴 수 있는 상태로 보존.

## 3. Rollout

운영 배포는 점진적으로 진행한다.

### 3.1 단계별 진행

1. **스테이징 검증**
   - 동일 빌드를 스테이징에 배포.
   - 스모크 케이스: `성수 두쫀쿠`, `시오빵`, `asdfqwer` (NONE_DETECTED).
   - `search_retrieval` 로그가 정상 출력되는지 확인.
2. **프로덕션 카나리 (가능한 경우)**
   - 일부 인스턴스/일부 트래픽에 우선 적용.
   - 첫 30분: 5xx 비율, zero-result rate, guard rejection 분포 확인.
3. **프로덕션 전체 적용**
   - 카나리 정상 시 전체 롤아웃.
   - 첫 60분: §4 모니터링 항목 모두 정상 범위 확인.

### 3.2 변경 후 즉시 확인

- [ ] `search_retrieval` 로그가 분당 정상 양으로 흐른다.
- [ ] `RetrievalPipeline`의 `Vector search failed` WARN 빈도가 평소 수준.
- [ ] `키워드 추출 실패, NONE_DETECTED 폴백` WARN 빈도가 평소 수준.
- [ ] 사용자 검색 5xx 비율이 평소 수준.

## 4. Rollback 조건

다음 중 하나라도 발생하면 즉시 롤백을 고려한다. 셋 이상 동시 발생이면 무조건 롤백.

### 4.1 사용자 영향

- [ ] 검색 API 5xx 비율이 직전 1시간 평균 대비 2배 이상 증가.
- [ ] 검색 평균 응답 시간이 직전 1시간 평균 대비 2배 이상 증가.
- [ ] 결과 카드의 명백한 false positive 사용자 신고 발생.

### 4.2 품질 신호

- [ ] zero-result rate가 직전 7일 평균 +20%p 이상 증가하며 회복 기미가 없음.
- [ ] vector-only result rate가 일 평균 30%를 초과하여 지속.
- [ ] guard rejection rate가 0으로 수렴 (PRODUCT_ONLY / BOTH_DETECTED 케이스에서).
- [ ] `UNAVAILABLE_METADATA` reason이 비정상적으로 누적.

### 4.3 외부 의존

- [ ] Qdrant 자체가 장기 장애 상태이며 자체 복구가 60분 내 불가.
- [ ] Gemini 호출 실패율이 5%/min 초과하며 회복 기미가 없음. (현재 시스템은 자동 폴백되므로 즉시 롤백 대상은 아님 — 그러나 LLM 의존 기능이 광범위하게 무력화되면 롤백 검토)

## 5. Rollback 절차

1. 직전 안정 버전(이전 빌드 또는 이전 설정)으로 재배포.
2. 인덱스 swap이 동반된 변경이라면 이전 컬렉션명으로 되돌리고 신규 컬렉션 삭제는 검증 후로 미룬다.
3. 가드 임계치를 바꾼 변경이라면 이전 값으로 환경변수 복귀 후 재기동.
4. 롤백 30분 후 §3.2 항목 재확인.
5. 사후 회고:
   - 트리거된 롤백 조건과 시간선 기록.
   - golden CSV / 평가 harness에 회귀 케이스 추가.
   - 본 체크리스트의 미흡 항목 보강.

## 6. Ranking tuning entry criteria

검색 ranking weight 튜닝(예: reranker 가중치, 가드 임계치 미세조정)은 운영 신호가 쌓인 뒤에 진행한다. 다음 조건을 모두 만족할 때 튜닝 작업을 착수할 수 있다.

- [ ] 운영에서 `search_retrieval` 로그가 최소 2주 이상 정상 수집되었다.
- [ ] 사용자 engagement proxy(클릭/전환 등)가 검색 결과 단위로 집계 가능하다.
  - 본 PR 시점에는 아직 수집되지 않을 수 있다. 수집 파이프라인이 먼저다.
- [ ] golden CSV가 운영 query 분포를 충분히 대표한다 (zero-result 케이스, alias 케이스, 오타 케이스 포함).
- [ ] 가설 → 실험안 → 평가 지표 3종이 PR 본문/실험 계획서에 명시되어 있다.
- [ ] 평가 지표가 Recall@K / Precision@K / engagement proxy 중 최소 2종이며, 단일 지표만으로 결정하지 않는다.

위 조건을 만족하지 못한 상태에서의 ranking 변경은 회귀 위험이 크므로 거절한다.

## 7. 인덱스/임베딩 변경 절차 (참고)

벡터 차원/임베딩 모델/컬렉션 swap은 본 체크리스트의 가장 위험한 변경이다.

1. 신규 컬렉션을 `QDRANT_COLLECTION_NAME`과 다른 이름으로 생성.
2. 전체 적재 완료 후 evaluation harness로 품질 검증.
3. 트래픽 cut-over는 환경변수 변경 + 재기동으로 처리.
4. 이전 컬렉션은 최소 24시간 보존 (롤백용).
5. 안정 확인 후 이전 컬렉션 삭제.

## 8. 관련 문서

- `docs/search-quality-observability.md` — 로그/지표 정의 및 Quality Gate
- `docs/search-mvp-runbook.md` — 장애 대응 절차
