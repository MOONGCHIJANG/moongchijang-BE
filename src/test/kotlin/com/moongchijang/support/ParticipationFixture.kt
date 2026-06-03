package com.moongchijang.support

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequest
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.participation.domain.entity.Participation
import com.moongchijang.domain.participation.domain.entity.ParticipationStatus
import com.moongchijang.domain.participation.domain.entity.PickupStatus
import com.moongchijang.domain.store.domain.entity.DistrictType
import com.moongchijang.domain.store.domain.entity.RegionType
import com.moongchijang.domain.store.domain.entity.Store
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

object ParticipationFixture {

    fun createParticipation(
        participationId: Long,
        groupBuyId: Long,
        quantity: Int,
        totalAmount: Int,
        currentQuantity: Int,
        targetQuantity: Int,
        deadline: LocalDateTime,
        pickupDate: LocalDate,
        pickupTimeStart: LocalTime,
        createdAt: LocalDateTime,
        participationStatus: ParticipationStatus = ParticipationStatus.PAID_WAITING_GOAL,
        pickupStatus: PickupStatus = PickupStatus.NOT_READY,
        groupBuyStatus: GroupBuyStatus = GroupBuyStatus.IN_PROGRESS
    ): Participation {
        val user = UserFixture.createKakaoUser(id = 1L, nickname = "테스터")
        val groupBuy = createGroupBuy(
            groupBuyId = groupBuyId,
            currentQuantity = currentQuantity,
            targetQuantity = targetQuantity,
            deadline = deadline,
            pickupDate = pickupDate,
            pickupTimeStart = pickupTimeStart,
            groupBuyStatus = groupBuyStatus
        )

        return Participation(
            user = user,
            groupBuy = groupBuy,
            quantity = quantity,
            productAmount = totalAmount,
            feeAmount = 0,
            totalAmount = totalAmount,
            status = participationStatus,
            pickupStatus = pickupStatus
        ).apply {
            id = participationId
            setCreatedAt(this, createdAt)
        }
    }

    private fun createGroupBuy(
        groupBuyId: Long,
        currentQuantity: Int,
        targetQuantity: Int,
        deadline: LocalDateTime,
        pickupDate: LocalDate,
        pickupTimeStart: LocalTime,
        groupBuyStatus: GroupBuyStatus
    ): GroupBuy {
        val store = Store(
            name = "사이드템포",
            address = "서울 강남구 OO길 1",
            region = RegionType.SEOUL,
            district = DistrictType.SEOUL_GANGNAM_YEOKSAM_SAMSEONG
        )
        val request = GroupBuyRequest(
            user = com.moongchijang.support.UserFixture.createKakaoUser(id = 1L),
            storeName = "사이드템포",
            productName = "두쫀쿠 오리지널 1개",
            desiredQuantity = 50,
            desiredPickupDate = pickupDate
        )
        return GroupBuy(
            store = store,
            groupBuyRequest = request,
            productName = "두쫀쿠 오리지널 1개",
            productDescription = "테스트 상품 설명",
            price = 18000,
            targetQuantity = targetQuantity,
            currentQuantity = currentQuantity,
            maxQuantity = 100,
            status = groupBuyStatus,
            recruitmentStartAt = deadline.minusDays(1),
            deadline = deadline,
            pickupDate = pickupDate,
            pickupTimeStart = pickupTimeStart,
            pickupTimeEnd = pickupTimeStart.plusHours(4),
            pickupLocation = "매장 앞"
        ).apply {
            id = groupBuyId
        }
    }

    private fun setCreatedAt(participation: Participation, createdAt: LocalDateTime) {
        val field = participation.javaClass.superclass.getDeclaredField("createdAt")
        field.isAccessible = true
        field.set(participation, createdAt)
    }
}
