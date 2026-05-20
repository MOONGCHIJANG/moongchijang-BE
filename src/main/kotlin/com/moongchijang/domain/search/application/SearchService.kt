package com.moongchijang.domain.search.application

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
    private val searchHistoryRepository: SearchHistoryRepository,
    private val storeSearchService: StoreSearchService,
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val CACHE_TTL_MINUTES = 10L  // keys(*) 블로킹 대신 짧은 TTL로 freshness 확보
        private fun cacheKey(query: String) = "search:result:${sha256(query)}"

        private fun sha256(value: String): String =
            MessageDigest.getInstance("SHA-256")
                .digest(value.toByteArray())
                .joinToString("") { "%02x".format(it) }
    }

    fun search(query: String, userId: Long?): SearchResponse {
        val cacheKey = cacheKey(query)
        val cached = redisTemplate.opsForValue().get(cacheKey)
        if (cached != null) return objectMapper.readValue(cached, SearchResponse::class.java)

        userId?.let { searchHistoryRepository.save(it, query) }

        val response = enrichWithRecommendation(query, fullTextSearchEngine.search(query))

        redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(response), Duration.ofMinutes(CACHE_TTL_MINUTES))
        return response
    }

    /**
     * 결과 0건(EMPTY_CAN_REQUEST)일 때 Naver Local 매장 후보로 응답을 보강한다.
     * Naver 호출 실패는 검색 전체를 깨뜨리지 않도록 empty list 로 graceful degrade.
     * cold-start flywheel: 사용자가 추천 매장에 공구 요청 → 다음 검색은 FULLTEXT 엔진이 잡음.
     */
    private fun enrichWithRecommendation(query: String, response: SearchResponse): SearchResponse {
        if (response.uiState != SearchUiState.EMPTY_CAN_REQUEST) return response
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
}
