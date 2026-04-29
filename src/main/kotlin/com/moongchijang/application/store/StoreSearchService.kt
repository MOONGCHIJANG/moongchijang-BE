package com.moongchijang.application.store

import com.moongchijang.application.store.dto.StoreSearchResponse
import com.moongchijang.infrastructure.naver.NaverLocalSearchClient
import org.springframework.stereotype.Service

@Service
class StoreSearchService(
    private val naverLocalSearchClient: NaverLocalSearchClient
) {
    fun search(keyword: String): StoreSearchResponse {
        val response = naverLocalSearchClient.search(keyword)
        return StoreSearchResponse.from(response.items)
    }
}
