package com.moongchijang.domain.search.application

import com.moongchijang.domain.search.application.dto.GroupBuyCardDto
import com.moongchijang.domain.search.application.dto.SearchCase
import com.moongchijang.domain.search.application.dto.SearchResponse
import com.moongchijang.domain.search.domain.SearchUiState
import com.moongchijang.domain.search.infrastructure.SearchHistoryRepository
import com.moongchijang.domain.store.application.StoreSearchService
import com.moongchijang.domain.store.application.dto.StoreSearchResponse
import com.moongchijang.global.config.SearchProperties
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import tools.jackson.databind.ObjectMapper
import java.time.Duration

class SearchServiceTest {

    private val searchOrchestrator: SearchOrchestrator = Mockito.mock(SearchOrchestrator::class.java)
    private val fullTextSearchEngine: FullTextSearchEngine = Mockito.mock(FullTextSearchEngine::class.java)
    private val searchHistoryRepository: SearchHistoryRepository = Mockito.mock(SearchHistoryRepository::class.java)
    private val searchIndexVersionService: SearchIndexVersionService = Mockito.mock(SearchIndexVersionService::class.java)
    private val storeSearchService: StoreSearchService = Mockito.mock(StoreSearchService::class.java)
    private val redisTemplate: StringRedisTemplate = Mockito.mock(StringRedisTemplate::class.java)

    @Suppress("UNCHECKED_CAST")
    private val valueOperations: ValueOperations<String, String> =
        Mockito.mock(ValueOperations::class.java) as ValueOperations<String, String>

    private val objectMapper = ObjectMapper()

    private val service = SearchService(
        searchOrchestrator = searchOrchestrator,
        fullTextSearchEngine = fullTextSearchEngine,
        searchHistoryRepository = searchHistoryRepository,
        searchIndexVersionService = searchIndexVersionService,
        storeSearchService = storeSearchService,
        searchProperties = SearchProperties(engine = "fulltext"),
        redisTemplate = redisTemplate,
        objectMapper = objectMapper
    )

    @BeforeEach
    fun setUp() {
        Mockito.`when`(redisTemplate.opsForValue()).thenReturn(valueOperations)
        Mockito.`when`(valueOperations.get(anyString())).thenReturn(null)
        Mockito.`when`(searchIndexVersionService.currentVersion()).thenReturn("v1")
    }

    private fun emptyResponse() = SearchResponse(
        searchCase = SearchCase.NONE_DETECTED,
        detectedRegion = null,
        detectedProduct = null,
        confidence = 0.0,
        uiState = SearchUiState.EMPTY_CAN_REQUEST,
        totalCount = 0,
        results = emptyList()
    )

    private fun resultsResponse() = SearchResponse(
        searchCase = SearchCase.NONE_DETECTED,
        detectedRegion = null,
        detectedProduct = null,
        confidence = 0.0,
        uiState = SearchUiState.RESULTS,
        totalCount = 1,
        results = listOf<GroupBuyCardDto>()
    )

    private fun storeItem(name: String) = StoreSearchResponse.StoreItem(
        placeId = "p-$name",
        storeName = name,
        roadAddress = "서울 어딘가",
        lotAddress = null,
        latitude = 37.5,
        longitude = 127.0
    )

    @Test
    @DisplayName("EMPTY_CAN_REQUEST면 Naver Local 매장 후보로 recommendedStores를 채운다")
    fun `empty response is enriched with naver recommendations`() {
        Mockito.`when`(fullTextSearchEngine.search("없는상품")).thenReturn(emptyResponse())
        val recs = StoreSearchResponse(stores = listOf(storeItem("동네빵집"), storeItem("뒷골목빵집")))
        Mockito.`when`(storeSearchService.search("없는상품")).thenReturn(recs)

        val response = service.search("없는상품", userId = null)

        assertThat(response.uiState).isEqualTo(SearchUiState.EMPTY_CAN_REQUEST)
        assertThat(response.recommendedStores).extracting("storeName")
            .containsExactly("동네빵집", "뒷골목빵집")
    }

    @Test
    @DisplayName("EMPTY_CAN_REQUEST에서 Naver 호출이 실패해도 검색 전체는 깨지지 않고 빈 리스트로 응답한다")
    fun `naver failure degrades to empty recommendations`() {
        Mockito.`when`(fullTextSearchEngine.search("없는상품")).thenReturn(emptyResponse())
        Mockito.`when`(storeSearchService.search("없는상품"))
            .thenThrow(CustomException(ErrorCode.STORE_SEARCH_FAILED))

        val response = service.search("없는상품", userId = null)

        assertThat(response.uiState).isEqualTo(SearchUiState.EMPTY_CAN_REQUEST)
        assertThat(response.recommendedStores).isEmpty()
    }

    @Test
    @DisplayName("RESULTS 응답에는 Naver를 호출하지 않고 recommendedStores는 null로 유지된다")
    fun `results response does not call naver`() {
        Mockito.`when`(fullTextSearchEngine.search("소금빵")).thenReturn(resultsResponse())

        val response = service.search("소금빵", userId = null)

        assertThat(response.uiState).isEqualTo(SearchUiState.RESULTS)
        assertThat(response.recommendedStores).isNull()
        Mockito.verifyNoInteractions(storeSearchService)
    }

    @Test
    @DisplayName("enrich된 응답이 그대로 Redis에 캐시된다")
    fun `enriched response is cached`() {
        Mockito.`when`(fullTextSearchEngine.search("없는상품")).thenReturn(emptyResponse())
        Mockito.`when`(storeSearchService.search("없는상품"))
            .thenReturn(StoreSearchResponse(stores = listOf(storeItem("동네빵집"))))

        service.search("없는상품", userId = null)

        Mockito.verify(valueOperations).set(
            anyString(),
            Mockito.contains("동네빵집"),
            Mockito.eq(Duration.ofMinutes(10L))
        )
    }
}
