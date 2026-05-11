package com.moongchijang.domain.search.application

import com.moongchijang.domain.search.application.dto.SearchResponse
import com.moongchijang.domain.search.infrastructure.SearchHistoryRepository
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.fasterxml.jackson.databind.ObjectMapper
import java.security.MessageDigest
import java.time.Duration

@Service
@Transactional(readOnly = true)
class SearchService(
    private val searchOrchestrator: SearchOrchestrator,
    private val searchHistoryRepository: SearchHistoryRepository,
    private val searchIndexVersionService: SearchIndexVersionService,
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper
) {
    companion object {
        private const val CACHE_TTL_MINUTES = 10L  // keys(*) 블로킹 대신 짧은 TTL로 freshness 확보
        private const val SEARCH_VERSION = "hybrid-v1"
        private fun cacheKey(query: String, indexVersion: String) =
            "search:result:$SEARCH_VERSION:$indexVersion:${sha256(query)}"

        private fun sha256(value: String): String =
            MessageDigest.getInstance("SHA-256")
                .digest(value.toByteArray())
                .joinToString("") { "%02x".format(it) }
    }

    fun search(query: String, userId: Long?): SearchResponse {
        val indexVersion = searchIndexVersionService.currentVersion()
        val cacheKey = cacheKey(query, indexVersion)
        val cached = redisTemplate.opsForValue().get(cacheKey)
        if (cached != null) return objectMapper.readValue(cached, SearchResponse::class.java)

        userId?.let { searchHistoryRepository.save(it, query) }

        val response = searchOrchestrator.search(query)

        redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(response), Duration.ofMinutes(CACHE_TTL_MINUTES))
        return response
    }

    fun getHistory(userId: Long): List<String> = searchHistoryRepository.getHistory(userId)

    fun clearHistory(userId: Long) = searchHistoryRepository.clear(userId)

    fun deleteHistory(userId: Long, keyword: String) = searchHistoryRepository.deleteOne(userId, keyword)
}
