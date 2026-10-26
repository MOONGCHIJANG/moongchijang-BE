package com.moongchijang.domain.search.application

import com.moongchijang.domain.product.application.ProductFeedQueryService
import com.moongchijang.domain.product.application.dto.ProductFeedItemResponse
import com.moongchijang.domain.product.application.dto.ProductFeedRequest
import com.moongchijang.domain.product.application.dto.ProductFeedResponse
import com.moongchijang.domain.product.application.dto.ProductFeedStoreResponse
import com.moongchijang.domain.product.domain.entity.ProductStorageType
import com.moongchijang.domain.product.domain.repository.ProductFeedSort
import com.moongchijang.domain.search.application.dto.ProductSearchRequest
import com.moongchijang.domain.search.application.dto.SearchCase
import com.moongchijang.domain.search.application.dto.SearchResponse
import com.moongchijang.domain.search.domain.SearchUiState
import com.moongchijang.domain.search.infrastructure.SearchHistoryRepository
import com.moongchijang.domain.store.application.StoreSearchService
import com.moongchijang.domain.store.application.dto.StoreSearchResponse
import com.moongchijang.domain.store.domain.entity.DistrictType
import com.moongchijang.domain.store.domain.entity.RegionType
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import tools.jackson.databind.ObjectMapper
import java.time.Duration
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class SearchServiceTest {

    @Mock
    private lateinit var fullTextSearchEngine: FullTextSearchEngine

    @Mock
    private lateinit var productFeedQueryService: ProductFeedQueryService

    @Mock
    private lateinit var searchHistoryRepository: SearchHistoryRepository

    @Mock
    private lateinit var storeSearchService: StoreSearchService

    @Mock
    private lateinit var redisTemplate: StringRedisTemplate

    @Mock
    private lateinit var valueOperations: ValueOperations<String, String>

    private val objectMapper = ObjectMapper()
    private lateinit var service: SearchService

    @BeforeEach
    fun setUp() {
        Mockito.`when`(redisTemplate.opsForValue()).thenReturn(valueOperations)
        Mockito.`when`(valueOperations.get(anyString())).thenReturn(null)
        service = SearchService(
            fullTextSearchEngine = fullTextSearchEngine,
            productFeedQueryService = productFeedQueryService,
            searchHistoryRepository = searchHistoryRepository,
            storeSearchService = storeSearchService,
            redisTemplate = redisTemplate,
            objectMapper = objectMapper,
        )
    }

    @Test
    fun `검색 조건과 후보 상품을 상품 피드 조회에 전달한다`() {
        val request = ProductSearchRequest(
            keyword = "소금빵",
            categoryCodes = setOf("BAKERY"),
            districts = setOf(DistrictType.SEOUL_GANGNAM_YEOKSAM_SAMSEONG),
            smallQuantity = true,
            sort = ProductFeedSort.PRICE_LOW,
            cursor = "PRICE_LOW|1000|1",
            limit = 10,
        )
        val match = ProductSearchMatch(
            searchCase = SearchCase.PRODUCT_ONLY,
            detectedRegion = null,
            detectedProduct = "소금빵",
            confidence = 0.5,
            productIds = setOf(1L, 2L),
        )
        val feedRequest = ProductFeedRequest(
            categoryCodes = request.categoryCodes,
            districts = request.districts,
            smallQuantity = true,
            sort = ProductFeedSort.PRICE_LOW,
            cursor = request.cursor,
            limit = 10,
        )
        Mockito.`when`(fullTextSearchEngine.search("소금빵")).thenReturn(match)
        Mockito.`when`(productFeedQueryService.getFeed(feedRequest, 42L, match.productIds))
            .thenReturn(ProductFeedResponse(emptyList(), null, false))
        val result = service.search(request, userId = 42L)

        Mockito.verify(searchHistoryRepository).save(42L, "소금빵")
        Mockito.verify(productFeedQueryService).getFeed(feedRequest, 42L, match.productIds)
        assertThat(result.uiState).isEqualTo(SearchUiState.RESULTS)
        Mockito.verifyNoInteractions(storeSearchService)
    }

    @Test
    fun `검색 후보가 없으면 추천 매장을 반환한다`() {
        Mockito.`when`(fullTextSearchEngine.search("없는상품")).thenReturn(ProductSearchMatch.empty())
        Mockito.`when`(storeSearchService.search("없는상품"))
            .thenReturn(StoreSearchResponse(listOf(storeItem("동네빵집"))))

        val result = service.search(ProductSearchRequest("없는상품"), userId = null)

        assertThat(result.uiState).isEqualTo(SearchUiState.EMPTY_CAN_REQUEST)
        assertThat(result.recommendedStores).extracting("storeName").containsExactly("동네빵집")
        Mockito.verifyNoInteractions(productFeedQueryService)
    }

    @Test
    fun `상품 검색 결과와 다음 커서를 반환하고 추천 매장은 조회하지 않는다`() {
        val request = ProductSearchRequest("소금빵", limit = 1)
        val match = ProductSearchMatch(
            searchCase = SearchCase.PRODUCT_ONLY,
            detectedRegion = null,
            detectedProduct = "소금빵",
            confidence = 0.5,
            productIds = setOf(1L, 2L),
        )
        val item = productItem()
        Mockito.`when`(fullTextSearchEngine.search("소금빵")).thenReturn(match)
        Mockito.`when`(
            productFeedQueryService.getFeed(
                ProductFeedRequest(limit = 1),
                null,
                match.productIds,
            )
        ).thenReturn(ProductFeedResponse(listOf(item), "LATEST|2026-10-26T10:00:00|1", true))

        val result = service.search(request, userId = null)

        assertThat(result.uiState).isEqualTo(SearchUiState.RESULTS)
        assertThat(result.results).containsExactly(item)
        assertThat(result.nextCursor).isEqualTo("LATEST|2026-10-26T10:00:00|1")
        assertThat(result.hasNext).isTrue()
        Mockito.verifyNoInteractions(storeSearchService)
    }

    @Test
    fun `마지막 커서 이후 결과가 비어도 추천 매장을 조회하지 않는다`() {
        val request = ProductSearchRequest(
            keyword = "소금빵",
            cursor = "LATEST|2026-10-26T10:00:00|1",
        )
        val match = ProductSearchMatch(
            searchCase = SearchCase.PRODUCT_ONLY,
            detectedRegion = null,
            detectedProduct = "소금빵",
            confidence = 0.5,
            productIds = setOf(1L),
        )
        Mockito.`when`(fullTextSearchEngine.search("소금빵")).thenReturn(match)
        Mockito.`when`(
            productFeedQueryService.getFeed(
                ProductFeedRequest(cursor = request.cursor),
                null,
                match.productIds,
            )
        ).thenReturn(ProductFeedResponse(emptyList(), null, false))

        val result = service.search(request, userId = null)

        assertThat(result.uiState).isEqualTo(SearchUiState.RESULTS)
        assertThat(result.results).isEmpty()
        Mockito.verifyNoInteractions(storeSearchService)
    }

    @Test
    fun `추천 매장 조회 실패는 빈 추천으로 처리한다`() {
        Mockito.`when`(fullTextSearchEngine.search("없는상품")).thenReturn(ProductSearchMatch.empty())
        Mockito.`when`(storeSearchService.search("없는상품"))
            .thenThrow(CustomException(ErrorCode.STORE_SEARCH_FAILED))

        val result = service.search(ProductSearchRequest("없는상품"), userId = null)

        assertThat(result.recommendedStores).isEmpty()
    }

    @Test
    fun `캐시 응답을 사용해도 최근 검색어는 기록한다`() {
        val cached = SearchResponse(
            searchCase = SearchCase.NONE_DETECTED,
            detectedRegion = null,
            detectedProduct = null,
            uiState = SearchUiState.EMPTY_CAN_REQUEST,
            totalCount = 0,
            results = emptyList(),
        )
        Mockito.`when`(valueOperations.get(anyString())).thenReturn(objectMapper.writeValueAsString(cached))

        service.search(ProductSearchRequest("소금빵"), userId = 42L)

        Mockito.verify(searchHistoryRepository).save(42L, "소금빵")
        Mockito.verifyNoInteractions(fullTextSearchEngine)
    }

    @Test
    fun `사용자와 필터가 반영된 검색 응답을 캐시한다`() {
        Mockito.`when`(fullTextSearchEngine.search("소금빵")).thenReturn(ProductSearchMatch.empty())
        Mockito.`when`(storeSearchService.search("소금빵")).thenReturn(StoreSearchResponse(emptyList()))

        service.search(ProductSearchRequest("소금빵", newOnly = true), userId = 42L)

        Mockito.verify(valueOperations).set(
            anyString(),
            Mockito.contains("EMPTY_CAN_REQUEST"),
            Mockito.eq(Duration.ofMinutes(10)),
        )
    }

    private fun storeItem(name: String) = StoreSearchResponse.StoreItem(
        placeId = "p-$name",
        storeName = name,
        roadAddress = "서울 어딘가",
        lotAddress = null,
        latitude = 37.5,
        longitude = 127.0,
        imageUrl = "https://cdn.example.com/$name.jpg",
    )

    private fun productItem() = ProductFeedItemResponse(
        productId = 1L,
        categoryCode = "BAKERY",
        name = "소금빵",
        thumbnailUrl = "https://cdn.example.com/salt-bread.jpg",
        unitPrice = 3_000,
        minQuantity = 10,
        maxQuantity = 100,
        storageType = ProductStorageType.ROOM_TEMPERATURE,
        store = ProductFeedStoreResponse(
            storeId = 10L,
            name = "동네빵집",
            region = RegionType.SEOUL,
            district = DistrictType.SEOUL_GANGNAM_YEOKSAM_SAMSEONG,
        ),
        earliestPickupDate = LocalDate.of(2026, 10, 28),
        isNew = true,
        isWishlisted = false,
        canPurchase = true,
        unavailableReason = null,
    )
}
