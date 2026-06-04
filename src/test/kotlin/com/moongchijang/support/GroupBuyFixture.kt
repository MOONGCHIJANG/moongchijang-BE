package com.moongchijang.support

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyImage
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequest
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.store.domain.entity.DistrictType
import com.moongchijang.domain.store.domain.entity.RegionType
import com.moongchijang.domain.store.domain.entity.Store
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

object GroupBuyFixture {

    fun createGroupBuy(
        id: Long,
        status: GroupBuyStatus,
        deadline: LocalDateTime = LocalDateTime.now().plusDays(3),
        productName: String = "두쫀쿠 1개",
        price: Int = 6000,
        targetQuantity: Int = 50,
        currentQuantity: Int = 36,
        maxQuantity: Int = 100
    ): GroupBuy {
        return GroupBuy(
            store = createStore(),
            groupBuyRequest = createGroupBuyRequest(productName = productName, desiredQuantity = targetQuantity),
            thumbnailKey = "dev/group-buys/test/thumbnail/example.jpg",
            productName = productName,
            productDescription = "설명",
            price = price,
            targetQuantity = targetQuantity,
            currentQuantity = currentQuantity,
            maxQuantity = maxQuantity,
            status = status,
            recruitmentStartAt = deadline.minusDays(3),
            deadline = deadline,
            pickupDate = LocalDate.now().plusDays(5),
            pickupTimeStart = LocalTime.of(14, 0),
            pickupTimeEnd = LocalTime.of(18, 0),
            pickupLocation = "서울 성동구 성수동",
            shareCount = 0
        ).apply { this.id = id }
    }

    fun createStore(
        name: String = "뭉치장 베이커리",
        address: String = "서울 성동구",
        region: RegionType = RegionType.SEOUL,
        district: DistrictType = DistrictType.SEOUL_SEONGSU_GEONDAE_GWANGJIN
    ): Store {
        return Store(
            name = name,
            address = address,
            region = region,
            district = district
        ).apply {
            id = 1L
            latitude = 37.544
            longitude = 127.055
        }
    }

    fun createGroupBuyRequest(
        userId: Long = 1L,
        storeName: String = "뭉치장 베이커리",
        storeAddress: String = "서울 성동구",
        productName: String = "두쫀쿠 1개",
        desiredQuantity: Int = 50,
        desiredPickupDate: LocalDate = LocalDate.now().plusDays(5)
    ): GroupBuyRequest {
        return GroupBuyRequest(
            user = com.moongchijang.support.UserFixture.createKakaoUser(id = userId),
            storeName = storeName,
            storeAddress = storeAddress,
            productName = productName,
            desiredQuantity = desiredQuantity,
            desiredPickupDate = desiredPickupDate
        ).apply { id = 20L }
    }

    fun createImage(groupBuy: GroupBuy, imageKey: String): GroupBuyImage {
        return GroupBuyImage(
            groupBuy = groupBuy,
            imageKey = imageKey
        )
    }
}
