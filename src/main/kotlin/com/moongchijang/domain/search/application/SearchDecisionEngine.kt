package com.moongchijang.domain.search.application

import com.moongchijang.domain.search.application.dto.SearchCase
import com.moongchijang.domain.search.domain.SearchIntent
import com.moongchijang.domain.search.domain.SearchUiState
import org.springframework.stereotype.Component

@Component
class SearchDecisionEngine {
    fun decide(intent: SearchIntent, resultCount: Int): SearchUiState {
        if (resultCount > 0) return SearchUiState.RESULTS

        return when (intent.searchCase) {
            SearchCase.BOTH_DETECTED -> SearchUiState.EMPTY_CAN_REQUEST
            SearchCase.PRODUCT_ONLY -> SearchUiState.NEED_REGION
            SearchCase.NEIGHBORHOOD_ONLY -> SearchUiState.NEED_PRODUCT
            SearchCase.NONE_DETECTED -> SearchUiState.NEED_BOTH
        }
    }
}
