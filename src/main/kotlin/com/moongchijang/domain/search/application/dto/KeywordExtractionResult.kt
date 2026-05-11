package com.moongchijang.domain.search.application.dto

data class KeywordExtractionResult(
    val region: String?,
    val product: String?,
    val searchCase: SearchCase
)
