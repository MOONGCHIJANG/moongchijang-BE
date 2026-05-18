package com.moongchijang.domain.search.application.dto

import com.moongchijang.domain.search.domain.SearchUiState
import com.moongchijang.domain.store.application.dto.StoreSearchResponse

data class SearchResponse(
    val searchCase: SearchCase,
    val detectedRegion: String?,
    val detectedProduct: String?,
    val confidence: Double = 0.0,
    val uiState: SearchUiState = SearchUiState.NEED_BOTH,
    val totalCount: Int,
    val results: List<GroupBuyCardDto>,
    val recommendedStores: List<StoreSearchResponse.StoreItem>? = null,
)
