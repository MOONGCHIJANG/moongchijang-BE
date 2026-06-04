package com.moongchijang.domain.store.infrastructure.naver

import com.moongchijang.domain.store.infrastructure.naver.dto.NaverLocalSearchResponse
import com.moongchijang.global.config.NaverApiProperties
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.system.measureTimeMillis

@Component
class NaverLocalSearchClient(
    private val naverApiProperties: NaverApiProperties,
    restClientBuilder: RestClient.Builder
) {
    private val restClient = restClientBuilder.build()
    private val cache = ConcurrentHashMap<CacheKey, CachedResponse>()
    private val log = LoggerFactory.getLogger(javaClass)

    fun search(keyword: String, display: Int = 5): NaverLocalSearchResponse {
        val normalizedKeyword = keyword.trim()
        val key = CacheKey(normalizedKeyword, display)
        val cached = cache[key]
        if (cached != null && cached.isFresh()) {
            log.debug(
                "[NaverLocalSearchClient] 로컬 검색 캐시 적중: keywordLength={}, display={}, total={}",
                normalizedKeyword.length,
                display,
                cached.response.total,
            )
            return cached.response
        }

        lateinit var response: NaverLocalSearchResponse
        val elapsedMs = measureTimeMillis {
            response = try {
                restClient.get()
                    .uri(
                        "${naverApiProperties.localSearchUrl}?query={keyword}&display={display}",
                        normalizedKeyword,
                        display
                    )
                    .header("X-Naver-Client-Id", naverApiProperties.clientId)
                    .header("X-Naver-Client-Secret", naverApiProperties.clientSecret)
                    .retrieve()
                    .body(NaverLocalSearchResponse::class.java)
                    ?: throw CustomException(ErrorCode.STORE_SEARCH_FAILED)
            } catch (e: RestClientException) {
                log.warn(
                    "[NaverLocalSearchClient] 로컬 검색 실패: keywordLength={}, display={}",
                    normalizedKeyword.length,
                    display,
                    e,
                )
                throw CustomException(ErrorCode.STORE_SEARCH_FAILED)
            }
        }
        if (elapsedMs >= SLOW_REQUEST_WARN_THRESHOLD_MS) {
            log.warn(
                "[NaverLocalSearchClient] 로컬 검색 지연: keywordLength={}, display={}, total={}, elapsedMs={}",
                normalizedKeyword.length,
                display,
                response.total,
                elapsedMs,
            )
        } else {
            log.debug(
                "[NaverLocalSearchClient] 로컬 검색 완료: keywordLength={}, display={}, total={}, elapsedMs={}",
                normalizedKeyword.length,
                display,
                response.total,
                elapsedMs,
            )
        }
        cacheSuccessfulResponse(key, response)
        return response
    }

    private fun cacheSuccessfulResponse(key: CacheKey, response: NaverLocalSearchResponse) {
        if (cache.size >= MAX_CACHE_SIZE) {
            cache.clear()
        }
        cache[key] = CachedResponse(response = response, expiresAt = Instant.now().plus(CACHE_TTL))
    }

    private data class CacheKey(
        val keyword: String,
        val display: Int,
    )

    private data class CachedResponse(
        val response: NaverLocalSearchResponse,
        val expiresAt: Instant,
    ) {
        fun isFresh(): Boolean = Instant.now().isBefore(expiresAt)
    }

    companion object {
        private val CACHE_TTL: Duration = Duration.ofMinutes(5)
        private const val MAX_CACHE_SIZE = 500
        private const val SLOW_REQUEST_WARN_THRESHOLD_MS = 1_000L
    }
}
