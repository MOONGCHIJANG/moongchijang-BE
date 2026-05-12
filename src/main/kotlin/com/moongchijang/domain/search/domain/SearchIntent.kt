package com.moongchijang.domain.search.domain

import com.moongchijang.domain.search.application.dto.SearchCase

data class SearchIntent(
    val region: String?,
    val product: String?,
    val searchCase: SearchCase,
    val confidence: Double
)
