package com.moongchijang.domain.search.application

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.search.application.dto.KeywordExtractionResult
import com.moongchijang.domain.search.application.dto.SearchCase
import com.moongchijang.domain.search.application.port.VectorSearchCandidate
import com.moongchijang.domain.search.application.port.VectorSearchPort
import com.moongchijang.domain.search.domain.SearchUiState
import com.moongchijang.domain.search.infrastructure.gemini.GeminiKeywordExtractionService
import com.moongchijang.domain.store.domain.entity.RegionType
import com.moongchijang.global.config.SearchProperties
import com.moongchijang.support.search.MockitoKotlinMatchers.anyGroupBuyStatus
import com.moongchijang.support.search.MockitoKotlinMatchers.anyLocalDateTime
import com.moongchijang.support.search.MockitoKotlinMatchers.anyLongList
import com.moongchijang.support.search.SearchTestFixtures
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito

class SearchOrchestratorIntegrationTest {

    private val groupBuyRepository: GroupBuyRepository = Mockito.mock(GroupBuyRepository::class.java)
    private val vectorSearchPort: VectorSearchPort = Mockito.mock(VectorSearchPort::class.java)
    private val keywordExtractor: GeminiKeywordExtractionService =
        Mockito.mock(GeminiKeywordExtractionService::class.java)

    private val aliasDictionary = AliasDictionary()
    private val reranker = SearchReranker()
    private val decisionEngine = SearchDecisionEngine()
    private val properties = SearchProperties()
    private val guard = VectorCandidatePromotionGuard(properties)
    private val observabilityLogger = SearchObservabilityLogger(properties)
    private val retrievalPipeline = RetrievalPipeline(
        groupBuyRepository = groupBuyRepository,
        vectorSearchPort = vectorSearchPort,
        aliasDictionary = aliasDictionary,
        vectorCandidatePromotionGuard = guard,
        reranker = reranker,
        searchObservabilityLogger = observabilityLogger,
        searchProperties = properties
    )
    private val orchestrator = SearchOrchestrator(
        groupBuyRepository = groupBuyRepository,
        keywordExtractor = keywordExtractor,
        aliasDictionary = aliasDictionary,
        retrievalPipeline = retrievalPipeline,
        decisionEngine = decisionEngine
    )

    private fun stubVocabulary(regions: List<String> = listOf("성수", "홍대"), products: List<String> = listOf("두쫀쿠", "소금빵")) {
        Mockito.`when`(groupBuyRepository.findDistinctRegions(GroupBuyStatus.IN_PROGRESS)).thenReturn(regions)
        Mockito.`when`(groupBuyRepository.findDistinctProductNames(GroupBuyStatus.IN_PROGRESS)).thenReturn(products)
    }

    private fun stubExact(result: List<GroupBuy>) {
        Mockito.`when`(
            groupBuyRepository.searchByIntent(
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
                anyLocalDateTime(),
                anyGroupBuyStatus()
            )
        ).thenReturn(result)
    }

    private fun stubVector(candidates: List<VectorSearchCandidate>) {
        Mockito.`when`(
            vectorSearchPort.search(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyInt(),
                ArgumentMatchers.anyDouble()
            )
        ).thenReturn(candidates)
    }

    private fun stubFindAllById(result: List<GroupBuy>) {
        Mockito.`when`(groupBuyRepository.findAllById(anyLongList())).thenReturn(result)
    }

    private fun stubExtraction(region: String?, product: String?) {
        val searchCase = when {
            region != null && product != null -> SearchCase.BOTH_DETECTED
            product != null -> SearchCase.PRODUCT_ONLY
            region != null -> SearchCase.NEIGHBORHOOD_ONLY
            else -> SearchCase.NONE_DETECTED
        }
        Mockito.`when`(
            keywordExtractor.extract(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyList(),
                ArgumentMatchers.anyList()
            )
        ).thenReturn(KeywordExtractionResult(region, product, searchCase))
    }

    @Test
    fun `Case 1 - BOTH_DETECTED 정확 매칭이 있으면 RESULTS와 결과 카드 반환`() {
        val match = SearchTestFixtures.groupBuy(id = 1L, productName = "두쫀쿠")
        stubVocabulary()
        stubExtraction(region = "성수", product = "두쫀쿠")
        stubExact(listOf(match))
        stubVector(emptyList())
        stubFindAllById(emptyList())

        val response = orchestrator.search("성수 두쫀쿠")

        assertThat(response.searchCase).isEqualTo(SearchCase.BOTH_DETECTED)
        assertThat(response.detectedRegion).isEqualTo("성수")
        assertThat(response.detectedProduct).isEqualTo("두쫀쿠")
        assertThat(response.confidence).isEqualTo(0.9)
        assertThat(response.uiState).isEqualTo(SearchUiState.RESULTS)
        assertThat(response.totalCount).isEqualTo(1)
        assertThat(response.results[0].id).isEqualTo(1L)
    }

    @Test
    fun `Case 2 - PRODUCT_ONLY alias 매칭 후 결과가 있으면 RESULTS 반환`() {
        val match = SearchTestFixtures.groupBuy(id = 1L, productName = "소금빵")
        stubVocabulary(products = listOf("소금빵"))
        // LLM은 추출 실패, alias dictionary가 시오빵 → 소금빵으로 보정
        Mockito.`when`(
            keywordExtractor.extract(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyList(),
                ArgumentMatchers.anyList()
            )
        ).thenReturn(KeywordExtractionResult(null, null, SearchCase.NONE_DETECTED))
        stubExact(listOf(match))
        stubVector(emptyList())
        stubFindAllById(emptyList())

        val response = orchestrator.search("시오빵")

        assertThat(response.detectedProduct).isEqualTo("소금빵")
        assertThat(response.searchCase).isEqualTo(SearchCase.PRODUCT_ONLY)
        assertThat(response.uiState).isEqualTo(SearchUiState.RESULTS)
        assertThat(response.totalCount).isEqualTo(1)
    }

    @Test
    fun `Case 3 - NEIGHBORHOOD_ONLY 결과가 있으면 RESULTS 반환`() {
        val match = SearchTestFixtures.groupBuy(id = 1L, store = SearchTestFixtures.store(region = RegionType.SEOUL))
        stubVocabulary()
        stubExtraction(region = "성수", product = null)
        stubExact(listOf(match))
        stubVector(emptyList())
        stubFindAllById(emptyList())

        val response = orchestrator.search("성수")

        assertThat(response.searchCase).isEqualTo(SearchCase.NEIGHBORHOOD_ONLY)
        assertThat(response.uiState).isEqualTo(SearchUiState.RESULTS)
        assertThat(response.totalCount).isEqualTo(1)
    }

    @Test
    fun `Case 4 - BOTH_DETECTED 결과 0건이면 EMPTY_CAN_REQUEST`() {
        stubVocabulary()
        stubExtraction(region = "성수", product = "두쫀쿠")
        stubExact(emptyList())
        stubVector(emptyList())
        stubFindAllById(emptyList())

        val response = orchestrator.search("성수 두쫀쿠")

        assertThat(response.uiState).isEqualTo(SearchUiState.EMPTY_CAN_REQUEST)
        assertThat(response.totalCount).isZero
    }

    @Test
    fun `NONE_DETECTED + 결과 0건이면 NEED_BOTH`() {
        stubVocabulary()
        stubExtraction(region = null, product = null)
        stubExact(emptyList())
        stubVector(emptyList())
        stubFindAllById(emptyList())

        val response = orchestrator.search("asdfqwer")

        assertThat(response.searchCase).isEqualTo(SearchCase.NONE_DETECTED)
        assertThat(response.confidence).isZero
        assertThat(response.uiState).isEqualTo(SearchUiState.NEED_BOTH)
        assertThat(response.totalCount).isZero
    }

    @Test
    fun `keywordExtractor가 예외를 던지면 NONE_DETECTED로 폴백한다`() {
        stubVocabulary()
        Mockito.`when`(
            keywordExtractor.extract(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyList(),
                ArgumentMatchers.anyList()
            )
        ).thenThrow(RuntimeException("LLM down"))
        stubExact(emptyList())
        stubVector(emptyList())
        stubFindAllById(emptyList())

        val response = orchestrator.search("성수 두쫀쿠")

        // LLM 폴백 후에도 aliasDictionary가 "두쫀쿠"를 유효 상품에서 fallback resolve → PRODUCT_ONLY
        assertThat(response.detectedRegion).isNull()
        assertThat(response.detectedProduct).isEqualTo("두쫀쿠")
        assertThat(response.searchCase).isEqualTo(SearchCase.PRODUCT_ONLY)
        assertThat(response.uiState).isEqualTo(SearchUiState.NEED_REGION)
    }
}
