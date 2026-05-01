package com.moongchijang.domain.store.infrastructure.naver

import com.moongchijang.domain.store.infrastructure.naver.dto.NaverLocalSearchResponse
import com.moongchijang.global.config.NaverApiProperties
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

@Component
class NaverLocalSearchClient(
    private val naverApiProperties: NaverApiProperties,
    restClientBuilder: RestClient.Builder
) {
    private val restClient = restClientBuilder.build()

    fun search(keyword: String, display: Int = 5): NaverLocalSearchResponse {
        return try {
            restClient.get()
                .uri("${naverApiProperties.localSearchUrl}?query={keyword}&display={display}",
                    keyword, display)
                .header("X-Naver-Client-Id", naverApiProperties.clientId)
                .header("X-Naver-Client-Secret", naverApiProperties.clientSecret)
                .retrieve()
                .body(NaverLocalSearchResponse::class.java)
                ?: throw CustomException(ErrorCode.STORE_SEARCH_FAILED)
        } catch (e: RestClientException) {
            throw CustomException(ErrorCode.STORE_SEARCH_FAILED)
        }
    }
}
