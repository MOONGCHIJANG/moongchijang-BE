package com.moongchijang.domain.store.application.dto

data class StoreSearchResponse(
    val stores: List<StoreItem>
) {
    data class StoreItem(
        val placeId: String,
        val storeName: String,
        val roadAddress: String,
        val lotAddress: String?,
        val latitude: Double,
        val longitude: Double,
        val imageUrl: String? = null
    )
}
