package com.moongchijang.domain.groupbuy.application.dto

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus

object GroupBuyProgressCalculator {
    private val CLOSED_STATUSES = setOf(
        GroupBuyStatus.COMPLETED,
        GroupBuyStatus.FAILED,
        GroupBuyStatus.CLOSED
    )

    fun achievementRate(currentQuantity: Int, targetQuantity: Int): Int {
        if (targetQuantity <= 0) return 0
        return ((currentQuantity * 100.0) / targetQuantity).toInt()
    }

    fun isClosed(groupBuy: GroupBuy): Boolean {
        return groupBuy.status in CLOSED_STATUSES
    }

    fun isParticipationOpenStatus(groupBuy: GroupBuy): Boolean {
        return groupBuy.status == GroupBuyStatus.IN_PROGRESS || groupBuy.status == GroupBuyStatus.ACHIEVED
    }
}
