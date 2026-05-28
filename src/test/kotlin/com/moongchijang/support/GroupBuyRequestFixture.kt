package com.moongchijang.support

import com.moongchijang.domain.groupbuy.application.dto.GroupBuyRequestCreateRequest
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequest
import java.time.LocalDate

object GroupBuyRequestFixture {

    fun createRequest(
        storeName: String = "성심당",
        storeAddress: String? = null,
        placeId: String? = null,
        roadAddress: String? = null,
        lotAddress: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        productName: String = "튀김소보로",
        desiredQuantity: Int = 2,
        desiredPickupDate: LocalDate = LocalDate.now().plusDays(3)
    ): GroupBuyRequestCreateRequest {
        return GroupBuyRequestCreateRequest(
            storeName = storeName,
            storeAddress = storeAddress,
            placeId = placeId,
            roadAddress = roadAddress,
            lotAddress = lotAddress,
            latitude = latitude,
            longitude = longitude,
            productName = productName,
            desiredQuantity = desiredQuantity,
            desiredPickupDate = desiredPickupDate
        )
    }

    fun createEntity(
        userId: Long,
        storeName: String,
        productName: String,
        desiredQuantity: Int,
        desiredPickupDate: LocalDate,
        storeAddress: String? = null
    ): GroupBuyRequest {
        return GroupBuyRequest(
            userId = userId,
            storeName = storeName,
            storeAddress = storeAddress,
            productName = productName,
            desiredQuantity = desiredQuantity,
            desiredPickupDate = desiredPickupDate
        )
    }
}
