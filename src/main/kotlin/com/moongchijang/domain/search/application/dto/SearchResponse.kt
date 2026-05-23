package com.moongchijang.domain.search.application.dto

import com.moongchijang.domain.groupbuy.application.dto.GroupBuyFeedItemResponse
import com.moongchijang.domain.search.domain.SearchUiState

data class SearchResponse(
    val searchCase: SearchCase,
    val detectedRegion: String?,
    val detectedProduct: String?,
    val confidence: Double = 0.0,
    val uiState: SearchUiState = SearchUiState.NEED_BOTH,
    val totalCount: Int,
    val results: List<GroupBuyFeedItemResponse>,
    val recommendedStores: List<RecommendedStoreDto>? = null,
)
