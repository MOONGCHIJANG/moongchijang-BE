package com.moongchijang.domain.groupbuy.application.dto

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequest
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.store.domain.entity.DistrictType
import com.moongchijang.domain.store.domain.entity.RegionType
import com.moongchijang.domain.store.domain.entity.Store
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class GroupBuyFeedItemResponseTest {

    @Test
    fun `피드 D-day는 시간 차이가 아니라 날짜 기준으로 계산한다`() {
        val now = LocalDateTime.of(2026, 5, 20, 16, 50)
        val groupBuy = createGroupBuy(
            deadline = LocalDateTime.of(2026, 5, 21, 6, 4, 50)
        )

        val response = GroupBuyFeedItemResponse.from(groupBuy, now)

        assertEquals(1, response.dDay)
        assertEquals("D-1", response.dDayLabel)
    }

    private fun createGroupBuy(deadline: LocalDateTime): GroupBuy {
        val store = Store(
            name = "청담 버터룸",
            address = "서울 강남구 도산대로 420",
            region = RegionType.SEOUL,
            district = DistrictType.SEOUL_SINSA_APGUJEONG_CHEONGDAM
        ).apply { id = 1L }
        val request = GroupBuyRequest(
            userId = 1L,
            storeName = "청담 버터룸",
            storeAddress = "서울 강남구 도산대로 420",
            productName = "버터바 4종 세트",
            desiredQuantity = 30,
            desiredPickupDate = LocalDate.of(2026, 5, 26)
        ).apply { id = 1L }

        return GroupBuy(
            store = store,
            groupBuyRequest = request,
            thumbnailUrl = "https://example.com/image.jpg",
            productName = "버터바 4종 세트",
            productDescription = "설명",
            price = 1000,
            targetQuantity = 30,
            currentQuantity = 28,
            maxQuantity = 45,
            status = GroupBuyStatus.IN_PROGRESS,
            deadline = deadline,
            pickupDate = LocalDate.of(2026, 5, 26),
            pickupTimeStart = LocalTime.of(12, 0),
            pickupTimeEnd = LocalTime.of(16, 0),
            pickupLocation = "서울 강남구 도산대로 420"
        ).apply { id = 901003L }
    }
}
