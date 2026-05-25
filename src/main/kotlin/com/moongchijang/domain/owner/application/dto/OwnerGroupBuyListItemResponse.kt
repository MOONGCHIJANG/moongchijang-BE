package com.moongchijang.domain.owner.application.dto

import com.moongchijang.domain.groupbuy.application.dto.GroupBuyProgressCalculator
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import java.time.LocalDate

data class OwnerGroupBuyListItemResponse(
    val groupBuyId: Long,
    val productName: String,
    val targetQuantity: Int,
    val currentQuantity: Int,
    val achievementRate: Int,
    val price: Int,
    val deadline: LocalDate,
    val status: OwnerGroupBuyStatusResponse
) {
    companion object {
        fun from(groupBuy: GroupBuy): OwnerGroupBuyListItemResponse =
            OwnerGroupBuyListItemResponse(
                groupBuyId = groupBuy.id,
                productName = groupBuy.productName,
                targetQuantity = groupBuy.targetQuantity,
                currentQuantity = groupBuy.currentQuantity,
                achievementRate = GroupBuyProgressCalculator.achievementRate(
                    currentQuantity = groupBuy.currentQuantity,
                    targetQuantity = groupBuy.targetQuantity
                ),
                price = groupBuy.price,
                deadline = groupBuy.deadline.toLocalDate(),
                status = OwnerGroupBuyStatusResponse.from(groupBuy.status)
            )
    }
}

enum class OwnerGroupBuyStatusResponse {
    IN_PROGRESS,
    ACHIEVED,
    FAILED;

    companion object {
        fun from(status: GroupBuyStatus): OwnerGroupBuyStatusResponse =
            when (status) {
                GroupBuyStatus.IN_PROGRESS -> IN_PROGRESS
                GroupBuyStatus.ACHIEVED -> ACHIEVED
                GroupBuyStatus.FAILED -> FAILED
                GroupBuyStatus.COMPLETED -> ACHIEVED
                GroupBuyStatus.CLOSED -> FAILED
            }
    }
}
