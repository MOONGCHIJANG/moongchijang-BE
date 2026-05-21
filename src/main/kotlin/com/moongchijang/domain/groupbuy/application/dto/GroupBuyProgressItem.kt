package com.moongchijang.domain.groupbuy.application.dto

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "다건 공구 달성률/참여 수량 아이템")
data class GroupBuyProgressItem(

    @field:Schema(description = "공구 ID", example = "101")
    val groupBuyId: Long,

    @field:Schema(description = "달성률(%)", example = "72")
    val achievementRate: Int,

    @field:Schema(description = "현재 참여 수량", example = "36")
    val currentQuantity: Int,

    @field:Schema(description = "목표 수량", example = "50")
    val targetQuantity: Int,

    @field:Schema(description = "마감 여부", example = "false")
    val isClosed: Boolean
) {
    companion object {
        fun from(groupBuy: GroupBuy, now: LocalDateTime = LocalDateTime.now()): GroupBuyProgressItem {
            val achievementRate = calculateAchievementRate(groupBuy.currentQuantity, groupBuy.targetQuantity)
            val isClosed = groupBuy.status != GroupBuyStatus.IN_PROGRESS || groupBuy.deadline.isBefore(now)

            return GroupBuyProgressItem(
                groupBuyId = groupBuy.id,
                achievementRate = achievementRate,
                currentQuantity = groupBuy.currentQuantity,
                targetQuantity = groupBuy.targetQuantity,
                isClosed = isClosed
            )
        }

        private fun calculateAchievementRate(currentQuantity: Int, targetQuantity: Int): Int {
            if (targetQuantity <= 0) return 0
            return ((currentQuantity * 100.0) / targetQuantity).toInt()
        }
    }
}
