package com.moongchijang.domain.search.application.dto

import com.moongchijang.domain.store.application.dto.StoreSearchResponse

/**
 * 검색 결과 0건(EMPTY_CAN_REQUEST) 응답에 동봉되는 동네 매장 추천 DTO.
 * store 도메인 DTO 를 직접 노출하지 않고 search 도메인 안에서 한 번 변환해
 * 외부(API 스펙) 결합도를 store 도메인 변경으로부터 격리한다.
 */
data class RecommendedStoreDto(
    val placeId: String,
    val storeName: String,
    val roadAddress: String,
    val lotAddress: String?,
    val latitude: Double,
    val longitude: Double,
) {
    companion object {
        fun from(item: StoreSearchResponse.StoreItem) = RecommendedStoreDto(
            placeId = item.placeId,
            storeName = item.storeName,
            roadAddress = item.roadAddress,
            lotAddress = item.lotAddress,
            latitude = item.latitude,
            longitude = item.longitude,
        )
    }
}
