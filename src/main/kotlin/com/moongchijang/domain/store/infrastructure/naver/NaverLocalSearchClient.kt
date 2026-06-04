package com.moongchijang.domain.store.infrastructure.naver

import com.moongchijang.domain.store.infrastructure.naver.dto.NaverLocalSearchResponse
import com.moongchijang.global.config.NaverApiProperties
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import tools.jackson.databind.ObjectMapper
import java.time.Duration

@Component
class NaverLocalSearchClient(
    private val naverApiProperties: NaverApiProperties,
    restClientBuilder: RestClient.Builder,
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) {
    private val restClient = restClientBuilder.build()
    private val log = LoggerFactory.getLogger(javaClass)

    fun search(keyword: String, display: Int = 5): NaverLocalSearchResponse {
        val normalizedKeyword = keyword.trim()
        val key = cacheKey(normalizedKeyword, display)

        readCache(key)?.let { cached -> return cached }

        val response = fetchFromNaver(normalizedKeyword, display)
        writeCache(key, response)
        return response
    }

    private fun fetchFromNaver(keyword: String, display: Int): NaverLocalSearchResponse {
        val startedAtMs = System.currentTimeMillis()
        val response = try {
            restClient.get()
                .uri(
                    "${naverApiProperties.localSearchUrl}?query={keyword}&display={display}",
                    keyword,
                    display
                )
                .header("X-Naver-Client-Id", naverApiProperties.clientId)
                .header("X-Naver-Client-Secret", naverApiProperties.clientSecret)
                .retrieve()
                .body(NaverLocalSearchResponse::class.java)
                ?: throw CustomException(ErrorCode.STORE_SEARCH_FAILED)
        } catch (e: RestClientException) {
            log.warn(
                "[NaverLocalSearchClient] 로컬 검색 실패: keywordLength={}, display={}, elapsedMs={}",
                keyword.length,
                display,
                System.currentTimeMillis() - startedAtMs,
                e,
            )
            throw CustomException(ErrorCode.STORE_SEARCH_FAILED)
        }
        val elapsedMs = System.currentTimeMillis() - startedAtMs
        if (elapsedMs >= SLOW_REQUEST_WARN_THRESHOLD_MS) {
            log.warn(
                "[NaverLocalSearchClient] 로컬 검색 지연: keywordLength={}, display={}, total={}, elapsedMs={}",
                keyword.length,
                display,
                response.total,
                elapsedMs,
            )
        }
        return response
    }

    private fun readCache(key: String): NaverLocalSearchResponse? {
        val raw = try {
            redisTemplate.opsForValue().get(key)
        } catch (e: DataAccessException) {
            log.warn("[NaverLocalSearchClient] Redis 캐시 조회 실패: key={}", key, e)
            return null
        } ?: return null

        return try {
            objectMapper.readValue(raw, NaverLocalSearchResponse::class.java)
        } catch (e: Exception) {
            log.warn("[NaverLocalSearchClient] 캐시 역직렬화 실패: key={}", key, e)
            runCatching { redisTemplate.delete(key) }
            null
        }
    }

    private fun writeCache(key: String, response: NaverLocalSearchResponse) {
        val raw = try {
            objectMapper.writeValueAsString(response)
        } catch (e: Exception) {
            log.warn("[NaverLocalSearchClient] 캐시 직렬화 실패: key={}", key, e)
            return
        }
        try {
            redisTemplate.opsForValue().set(key, raw, CACHE_TTL)
        } catch (e: DataAccessException) {
            log.warn("[NaverLocalSearchClient] Redis 캐시 저장 실패: key={}", key, e)
        }
    }

    private fun cacheKey(keyword: String, display: Int): String =
        "$CACHE_KEY_PREFIX:$display:$keyword"

    companion object {
        private const val CACHE_KEY_PREFIX = "store:naver-local-search"
        private val CACHE_TTL: Duration = Duration.ofMinutes(5)
        private const val SLOW_REQUEST_WARN_THRESHOLD_MS = 1_000L
    }
}
