package com.moongchijang.domain.search.domain

enum class SearchUiState {
    RESULTS,
    EMPTY_CAN_REQUEST,
    NEED_REGION,
    NEED_PRODUCT,
    NEED_BOTH,
    AMBIGUOUS_CONFIRMATION
}
