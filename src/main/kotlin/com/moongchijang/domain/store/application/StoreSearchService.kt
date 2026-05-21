package com.moongchijang.domain.store.application

import com.moongchijang.domain.store.application.dto.StoreSearchResponse
import com.moongchijang.domain.store.infrastructure.naver.NaverLocalSearchClient
import org.springframework.stereotype.Service

@Service
class StoreSearchService(
    private val naverLocalSearchClient: NaverLocalSearchClient
) {
    fun search(keyword: String, display: Int = 5): StoreSearchResponse {
        val response = naverLocalSearchClient.search(keyword, display)
        return StoreSearchResponse.from(response.items)
    }
}
