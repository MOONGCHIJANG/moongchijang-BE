package com.moongchijang.domain.search.application

import com.moongchijang.domain.product.application.ProductFeedQueryService
import com.moongchijang.domain.product.application.dto.ProductFeedRequest
import com.moongchijang.domain.search.application.dto.ProductSearchRequest
import com.moongchijang.domain.search.application.dto.RecommendedStoreDto
import com.moongchijang.domain.search.application.dto.SearchResponse
import com.moongchijang.domain.search.domain.SearchUiState
import com.moongchijang.domain.search.infrastructure.SearchHistoryRepository
import com.moongchijang.domain.store.application.StoreSearchService
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.security.MessageDigest
import java.time.Duration

@Service
@Transactional(readOnly = true)
class SearchService(
    private val fullTextSearchEngine: FullTextSearchEngine,
    private val productFeedQueryService: ProductFeedQueryService,
    private val searchHistoryRepository: SearchHistoryRepository,
    private val storeSearchService: StoreSearchService,
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun search(request: ProductSearchRequest, userId: Long?): SearchResponse {
        userId?.let { searchHistoryRepository.save(it, request.keyword) }

        val cacheKey = cacheKey(request, userId)
        redisTemplate.opsForValue().get(cacheKey)?.let {
            return objectMapper.readValue(it, SearchResponse::class.java)
        }

        val match = fullTextSearchEngine.search(request.keyword)
        val response = if (match.productIds.isEmpty()) {
            emptyResponse(match)
        } else {
            val feed = productFeedQueryService.getFeed(
                request = ProductFeedRequest(
                    categoryCodes = request.categoryCodes,
                    districts = request.districts,
                    pickupThisWeek = request.pickupThisWeek,
                    smallQuantity = request.smallQuantity,
                    newOnly = request.newOnly,
                    sort = request.sort,
                    cursor = request.cursor,
                    limit = request.limit,
                ),
                userId = userId,
                candidateProductIds = match.productIds,
            )
            SearchResponse(
                searchCase = match.searchCase,
                detectedRegion = match.detectedRegion,
                detectedProduct = match.detectedProduct,
                confidence = match.confidence,
                uiState = if (feed.items.isEmpty() && request.cursor == null) {
                    SearchUiState.EMPTY_CAN_REQUEST
                } else {
                    SearchUiState.RESULTS
                },
                totalCount = feed.items.size,
                results = feed.items,
                nextCursor = feed.nextCursor,
                hasNext = feed.hasNext,
            )
        }
        val enriched = enrichWithRecommendation(request.keyword, response)

        redisTemplate.opsForValue().set(
            cacheKey,
            objectMapper.writeValueAsString(enriched),
            Duration.ofMinutes(CACHE_TTL_MINUTES),
        )
        return enriched
    }

    /** 결과가 없을 때 Naver Local 매장 후보로 응답을 보강한다. */
    private fun enrichWithRecommendation(query: String, response: SearchResponse): SearchResponse {
        if (response.uiState != SearchUiState.EMPTY_CAN_REQUEST) {
            return response
        }
        val stores = try {
            storeSearchService.search(query).stores.map(RecommendedStoreDto::from)
        } catch (e: Exception) {
            log.warn("Naver Local 매장 추천 실패, 빈 추천으로 대체 query=$query", e)
            emptyList()
        }
        return response.copy(recommendedStores = stores)
    }

    fun getHistory(userId: Long): List<String> = searchHistoryRepository.getHistory(userId)

    fun clearHistory(userId: Long) = searchHistoryRepository.clear(userId)

    fun deleteHistory(userId: Long, keyword: String) = searchHistoryRepository.deleteOne(userId, keyword)

    private fun emptyResponse(match: ProductSearchMatch) = SearchResponse(
        searchCase = match.searchCase,
        detectedRegion = match.detectedRegion,
        detectedProduct = match.detectedProduct,
        confidence = match.confidence,
        uiState = SearchUiState.EMPTY_CAN_REQUEST,
        totalCount = 0,
        results = emptyList(),
    )

    private fun cacheKey(request: ProductSearchRequest, userId: Long?): String {
        val canonical = buildString {
            append(SearchQueryNormalizer.normalize(request.keyword))
            append('|').append(request.categoryCodes.map { it.trim().uppercase() }.sorted().joinToString(","))
            append('|').append(request.districts.map { it.name }.sorted().joinToString(","))
            append('|').append(request.pickupThisWeek)
            append('|').append(request.smallQuantity)
            append('|').append(request.newOnly)
            append('|').append(request.sort.name)
            append('|').append(request.cursor.orEmpty())
            append('|').append(request.limit)
            append('|').append(userId ?: ANONYMOUS_USER_ID)
        }
        return "search:result:${sha256(canonical)}"
    }

    companion object {
        private const val CACHE_TTL_MINUTES = 10L
        private const val ANONYMOUS_USER_ID = 0L

        private fun sha256(value: String): String =
            MessageDigest.getInstance("SHA-256")
                .digest(value.toByteArray())
                .joinToString("") { "%02x".format(it) }
    }
}
