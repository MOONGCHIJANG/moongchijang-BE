# 공구 검색 운영 런북

이 문서는 공구 검색이 운영에서 비정상 동작할 때 따라가는 대응 절차를 정의한다.
관측 데이터 정의/지표는 `docs/search-quality-observability.md`를 참조한다.

대상 독자: 검색 도메인 온콜, SRE.

## 1. 구성 요소 한눈 정리

| 컴포넌트 | 파일 | 역할 |
|----------|------|------|
| `SearchOrchestrator` | `domain/search/application/SearchOrchestrator.kt` | 검색 진입점. intent 추출 → retrieve → decision. |
| `GeminiKeywordExtractionService` | `domain/search/infrastructure/gemini/GeminiKeywordExtractionService.kt` | Gemini로 region/product 추출. 실패 시 NONE_DETECTED 폴백. |
| `AliasDictionary` | `domain/search/application/AliasDictionary.kt` | alias/오타 보정. LLM 실패 시 product fallback resolver 역할도 수행. |
| `RetrievalPipeline` | `domain/search/application/RetrievalPipeline.kt` | 정확 매칭 + 벡터 후보 + 가드 + 리랭킹. |
| `VectorCandidatePromotionGuard` | `domain/search/application/VectorCandidatePromotionGuard.kt` | 벡터 단독 false positive 차단. |
| `VectorSearchPort` 구현 | `domain/search/infrastructure/vector/qdrant/QdrantVectorSearchAdapter.kt` (운영), `MySqlVectorSearchAdapter.kt` (폴백 후보) | 벡터 검색. |
| `SearchObservabilityLogger` | `domain/search/application/SearchObservabilityLogger.kt` | `search_retrieval` 로그. |

## 2. 운영 환경 변수 / 설정 키

값 자체(API key, URL)는 문서에 적지 않는다. 변수명과 용도만 기록한다.

### 2.1 Gemini

| 키 (yml) | env | 용도 |
|----------|-----|------|
| `gemini.api-key` | `GEMINI_API_KEY` | Gemini API 인증. 부재 시 `GeminiKeywordExtractionService`가 호출 단계에서 예외 → NONE_DETECTED 폴백. |

### 2.2 Qdrant

| 키 (yml) | env | 용도 |
|----------|-----|------|
| `qdrant.enabled` | `QDRANT_ENABLED` | Qdrant 어댑터 활성화 여부 (기본 `false`). 운영에서는 `true`. |
| `qdrant.url` | `QDRANT_URL` | Qdrant 엔드포인트 |
| `qdrant.api-key` | `QDRANT_API_KEY` | Qdrant 인증. 비공개 클러스터면 비어도 됨. |
| `qdrant.collection-name` | `QDRANT_COLLECTION_NAME` | 컬렉션명. 인덱스 재구축 시 새 이름으로 만들고 cut-over. |
| `qdrant.timeout-seconds` | `QDRANT_TIMEOUT_SECONDS` | 호출 타임아웃. 초과 시 검색 단건이 벡터 없이 진행. |
| `qdrant.initialize-collection` | `QDRANT_INITIALIZE_COLLECTION` | 부팅 시 컬렉션 자동 생성. 운영에서는 의도적인 변경 시에만 `true`. |
| `qdrant.vector-size` | `QDRANT_VECTOR_SIZE` | 임베딩 차원. 모델 교체 시에만 변경. |
| `qdrant.distance` | `QDRANT_DISTANCE` | 거리 함수 (`Cosine` 기본). |

### 2.3 검색 동작

| 키 (yml) | env | 용도 |
|----------|-----|------|
| `search.retrieval.vector-candidate-limit` | `SEARCH_VECTOR_CANDIDATE_LIMIT` | Qdrant topK |
| `search.retrieval.vector-min-score` | `SEARCH_VECTOR_MIN_SCORE` | Qdrant minScore |
| `search.retrieval.fallback-provider` | `SEARCH_FALLBACK_PROVIDER` | 벡터 실패 시 fallback 식별자 (현재 로그에만 사용) |
| `search.guard.enabled` | `SEARCH_GUARD_ENABLED` | 가드 ON/OFF. 운영 기본 `true`. |
| `search.guard.min-confidence` | `SEARCH_GUARD_MIN_CONFIDENCE` | 가드 통과 confidence 최소치 |
| `search.guard.supportive-score-threshold` | `SEARCH_GUARD_SUPPORTIVE_SCORE_THRESHOLD` | NONE_DETECTED 보조 통과 score 임계 |
| `search.observability.enabled` | `SEARCH_OBSERVABILITY_ENABLED` | `search_retrieval` 로그 토글. 운영 기본 `true`. |
| `search.index.version-key` | `SEARCH_INDEX_VERSION_KEY` | 인덱스 버전 키 (Redis) |
| `search.index.default-version` | `SEARCH_INDEX_DEFAULT_VERSION` | 기본 인덱스 버전 |

**원칙**: 본 PR 또는 런북 작업 중에는 위 키들의 운영 값을 변경하지 않는다. 변경이 필요하면 별도 PR + 릴리스 체크리스트(§ release-checklist) 절차를 따른다.

## 3. 장애 대응 시나리오

### 3.1 Qdrant 장애 (네트워크/5xx/타임아웃)

**증상**
- `RetrievalPipeline`의 `Vector search failed, fallbackProvider=... error=...` WARN 로그가 다수 발생.
- `qdrantCandidateCount=0`이 지속적으로 관측됨.
- `providerSources`가 `MYSQL` 단독으로 수렴.

**현재 시스템 동작 (코드 보장)**

`RetrievalPipeline.searchVectorCandidates`는 `try/catch (Exception)` 처리되어 있다 (`RetrievalPipeline.kt:64-77`).
- Qdrant 호출 실패 → `emptyList()` 반환
- 결과: 벡터 후보 0건으로 진행 → MySQL 정확 매칭만 사용
- 사용자 응답은 정상 (다만 alias/유사도 기반 결과가 빠짐)

즉, Qdrant 장애는 **자동 graceful degradation**이며 사용자 검색이 5xx로 떨어지지 않는다.

**1차 대응**
1. Qdrant 인스턴스 health check.
2. `QDRANT_URL` 도달 가능성 확인.
3. WARN 로그 빈도와 `qdrantCandidateCount=0` 비율 추이를 본다.
4. 일시 장애면 자동 복구 후 정상화 확인.

**격리(intentional disable)가 필요할 때**
- `QDRANT_ENABLED=false`로 재기동하면 Qdrant 호출 자체가 비활성화 (`MySqlVectorSearchAdapter` 또는 no-op fallback 진입).
- 운영 yml을 직접 바꿀 때는 § release-checklist의 변경 절차를 따른다.

**복구 확인**
- `qdrantCandidateCount > 0`인 요청이 정상 수준으로 복귀.
- WARN 로그 멈춤.
- vector-only / hybrid 매치가 다시 관측됨.

### 3.2 Gemini 장애 (인증 실패/응답 지연/할당량 초과)

**증상**
- `키워드 추출 실패, NONE_DETECTED 폴백: query={}, error={}` WARN 로그 다수.
- `searchCase=NONE_DETECTED` 비율 급증.
- `confidenceBucket=NONE` 비율 급증.

**현재 시스템 동작 (코드 보장)**

`GeminiKeywordExtractionService.extract`는 try/catch로 감싸져 있다 (`GeminiKeywordExtractionService.kt:27-35`).
- 어떤 예외든 → `KeywordExtractionResult(null, null, NONE_DETECTED)` 반환.
- 이후 `SearchOrchestrator`에서 `AliasDictionary.resolveProduct` fallback이 query 토큰을 유효 상품 사전에서 fallback resolve한다.
- 따라서 LLM이 죽어도 alias 사전에 들어 있는 query는 PRODUCT_ONLY로 정상 해석된다 (참고: `SearchOrchestratorIntegrationTest` "keywordExtractor가 예외를 던지면 NONE_DETECTED로 폴백한다" 케이스가 이 경로를 검증).
- 결과: 검색 자체는 5xx로 떨어지지 않으며, 사용자 의도 추출 품질만 제한된다.

**1차 대응**
1. `GEMINI_API_KEY` 유효성/할당량 확인.
2. WARN 로그의 error 메시지로 원인 분류 (auth, quota, timeout).
3. 단기 복구 가능하면 그대로 둔다 (가드 + alias fallback으로 false positive는 차단됨).

**격리가 필요할 때**
- 현재 코드 경로상 Gemini 호출 자체를 끄는 별도 토글은 없다.
- **주의**: `GEMINI_API_KEY`를 비워서 LLM을 격리하려는 시도는 권장하지 않는다. 운영 yml(`application-prod.yml`)이 `gemini.api-key: ${GEMINI_API_KEY}`로 default 없이 필수 주입을 기대하기 때문에, 빈 값으로 기동하면 Spring 컨텍스트 로딩/`ChatModel` 빈 초기화 단계에서 실패해 **애플리케이션이 기동하지 않을 수 있다.** 운영 격리가 실제로 필요하다면 `gemini.enabled` 같은 전용 토글 도입(production 코드 변경, 본 PR 범위 밖)을 별도 작업으로 진행한다.
- 단기 우회가 꼭 필요하면 LLM 추출 결과를 무시하고 NONE_DETECTED로 동작하는 핫픽스를 별도 PR로 배포한다.
- 인덱스/사전 기반 검색만으로 운영하는 결정은 § release-checklist의 변경 절차를 따른다.

**복구 확인**
- `키워드 추출 실패` WARN 로그 멈춤.
- searchCase 분포가 BOTH_DETECTED / PRODUCT_ONLY / NEIGHBORHOOD_ONLY 비중 회복.

### 3.3 가드가 OFF로 운영 중인 상태 (사고)

**증상**
- `guardRejectedCount`가 0으로 수렴.
- `vector-only result rate` 급증.
- 사용자 신고: "전혀 관련 없는 결과가 나옵니다."

**대응**
1. 현재 적용된 `SEARCH_GUARD_ENABLED` 값을 확인한다.
2. `false`로 잘못 배포된 상태라면 `true`로 되돌리고 재기동.
3. 이번 PR 정책상 환경설정 변경은 별도 PR + 체크리스트.
4. 사고 후처리:
   - 노출된 false positive로 인한 사용자 피해 범위 확인.
   - 가드 토글이 의도치 않게 꺼진 경위 회고.

### 3.4 인덱스/메타데이터 불일치 (`UNAVAILABLE_METADATA` 증가)

**증상**
- `guardRejectionReasons`에 `UNAVAILABLE_METADATA` 카운트가 비정상적으로 높음.
- Qdrant에는 후보가 있지만 `groupBuyRepository.findAllById`가 비어 있는 상황.

**의미**
- Qdrant에 인덱싱된 `groupBuyId`가 DB에서 사라졌거나, 인덱스가 stale.

**대응**
1. 인덱스 버전 (`SEARCH_INDEX_DEFAULT_VERSION`)과 Qdrant 컬렉션 (`QDRANT_COLLECTION_NAME`)이 정합한지 확인.
2. 삭제된 공구가 Qdrant에 잔존하면 `QdrantVectorSearchAdapter`의 delete 경로 정상 동작 확인.
3. 대규모 stale이면 인덱스 재빌드 결정 (§ release-checklist의 인덱스 재구축 절차).

## 4. 상시 점검 체크 (주간)

- `search_retrieval` 로그가 정상 흐름인지 (분당 요청 수 모니터링).
- zero-result rate 7일 평균과 추세.
- vector-only result rate 7일 평균.
- guard rejection reason 분포 (NONE_DETECTED 외 reason이 급증하는지).
- Qdrant/Gemini WARN 로그 빈도.

## 5. 변경 시 안전 가이드

| 변경 대상 | 영향 | 사전 조치 |
|-----------|------|-----------|
| `search.guard.*` | false positive 노출/차단 비율 변동 | golden case로 회귀 평가 (§ release-checklist) |
| `search.retrieval.vector-*` | 후보 풀 규모/품질 변동 | top score 분포 baseline 재측정 |
| `qdrant.collection-name` | 인덱스 cut-over | 재인덱싱 완료 후 무중단 swap |
| `qdrant.vector-size` | 임베딩 차원 변경 → 컬렉션 재생성 필수 | 신규 컬렉션 빌드 후 swap |
| `gemini.api-key` | 모든 LLM 호출 차단 | NONE_DETECTED 폴백 비율 급증 예상 |
| `search.observability.enabled` | 관측 불능 | 운영에서는 끄지 않는다 |

## 6. 검증 명령

운영 점검/문제 진단에 쓰이는 로컬 검증 명령.

```bash
# 컴파일 체크 (빠름)
./gradlew classes testClasses

# 검색 도메인 단위/통합 테스트만 실행
./gradlew test --tests "com.moongchijang.domain.search.*"

# 가드 통합 테스트 (가드 ON/OFF 비교 시나리오 포함)
./gradlew test --tests "com.moongchijang.domain.search.application.RetrievalPipelineGuardIntegrationTest"

# Qdrant 통합 테스트 (QDRANT_URL 환경변수 있을 때만 실행)
QDRANT_URL=http://localhost:6333 ./gradlew test \
  --tests "com.moongchijang.domain.search.infrastructure.vector.qdrant.QdrantCrudIntegrationTest"

# Gemini 실 API 통합 테스트 (GEMINI_API_KEY 있을 때만 실행)
GEMINI_API_KEY=dummy-placeholder ./gradlew test \
  --tests "com.moongchijang.domain.search.infrastructure.gemini.RealGeminiKeywordExtractionTest"
```

`QDRANT_URL` / `GEMINI_API_KEY`는 placeholder 값으로 적었다. 실제 값은 로컬 `.env` 또는 운영 secret 매니저에서 가져온다. **secret을 본 문서/커밋에 포함하지 않는다.**

## 7. 관련 문서

- `docs/search-quality-observability.md` — 로그/지표 정의
- `docs/search-mvp-release-checklist.md` — 운영 변경 절차
