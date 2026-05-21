package com.moongchijang.domain.groupbuy.application.dto

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import java.time.LocalDateTime

object GroupBuyProgressCalculator {
    fun achievementRate(currentQuantity: Int, targetQuantity: Int): Int {
        if (targetQuantity <= 0) return 0
        return ((currentQuantity * 100.0) / targetQuantity).toInt()
    }

    fun isClosed(groupBuy: GroupBuy, now: LocalDateTime = LocalDateTime.now()): Boolean {
        return groupBuy.status != GroupBuyStatus.IN_PROGRESS || groupBuy.deadline.isBefore(now)
    }
}
