package com.moongchijang.support.search

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequest
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.store.domain.entity.DistrictType
import com.moongchijang.domain.store.domain.entity.RegionType
import com.moongchijang.domain.store.domain.entity.Store
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

object SearchTestFixtures {

    fun store(
        id: Long = 1L,
        name: String = "뭉치장 베이커리",
        region: RegionType = RegionType.SEOUL,
        district: DistrictType = DistrictType.SEOUL_SEONGSU_GEONDAE_GWANGJIN
    ): Store = Store(
        name = name,
        address = "서울 성동구",
        region = region,
        district = district
    ).apply { this.id = id }

    fun groupBuyRequest(id: Long = 100L): GroupBuyRequest =
        GroupBuyRequest(
            user = com.moongchijang.support.UserFixture.createKakaoUser(id = 1L),
            storeName = "뭉치장 베이커리",
            storeAddress = "서울 성동구",
            productName = "두쫀쿠",
            desiredQuantity = 50,
            desiredPickupDate = LocalDate.now().plusDays(5)
        ).apply { this.id = id }

    fun groupBuy(
        id: Long,
        productName: String = "두쫀쿠",
        store: Store = store(),
        status: GroupBuyStatus = GroupBuyStatus.IN_PROGRESS,
        deadline: LocalDateTime = LocalDateTime.now().plusDays(3),
    ): GroupBuy = GroupBuy(
        store = store,
        groupBuyRequest = groupBuyRequest(),
        thumbnailKey = "dev/group-buys/test/thumbnail/example.jpg",
        productName = productName,
        productDescription = "설명",
        price = 6000,
        targetQuantity = 50,
        currentQuantity = 10,
        maxQuantity = 100,
        status = status,
        deadline = deadline,
        pickupDate = LocalDate.now().plusDays(5),
        pickupTimeStart = LocalTime.of(14, 0),
        pickupTimeEnd = LocalTime.of(18, 0),
        pickupLocation = "서울 성동구 성수동",
        shareCount = 0
    ).apply { this.id = id }
}
