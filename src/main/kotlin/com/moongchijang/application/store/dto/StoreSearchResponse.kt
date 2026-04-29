package com.moongchijang.application.store.dto

import com.moongchijang.infrastructure.naver.dto.NaverLocalSearchItem

data class StoreSearchResponse(
    val stores: List<StoreItem>
) {
    data class StoreItem(
        val placeId: String,
        val storeName: String,
        val roadAddress: String,
        val lotAddress: String?,
        val latitude: Double,
        val longitude: Double
    )

    companion object {
        fun from(items: List<NaverLocalSearchItem>) = StoreSearchResponse(
            stores = items.map {
                StoreItem(
                    placeId = it.placeId(),
                    storeName = it.storeName(),
                    roadAddress = it.roadAddress,
                    lotAddress = it.address.ifBlank { null },
                    latitude = it.latitude(),
                    longitude = it.longitude()
                )
            }
        )
    }
}
