package com.moongchijang.domain.groupbuy.application.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class StoreRecommendationRequest(
    @field:NotBlank(message = "지역은 필수입니다")
    @field:Size(max = 50, message = "지역은 50자 이하이어야 합니다")
    val region: String,

    @field:NotBlank(message = "상품명은 필수입니다")
    @field:Size(max = 100, message = "상품명은 100자 이하이어야 합니다")
    val productName: String
)

data class StoreRecommendationResponse(
    val region: String,
    val productName: String,
    val stores: List<RecommendedStore>
) {
    data class RecommendedStore(
        val placeId: String,
        val storeName: String,
        val roadAddress: String,
        val lotAddress: String?,
        val latitude: Double,
        val longitude: Double,
        val category: String,
        val addressMatched: Boolean,
        val categoryMatched: Boolean,
        val registeredStore: Boolean,
        val previousGroupBuyStore: Boolean
    )
}
