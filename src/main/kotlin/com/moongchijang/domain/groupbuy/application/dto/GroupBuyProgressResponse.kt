package com.moongchijang.domain.groupbuy.application.dto

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "단일 공구 달성률/참여 수량 응답")
data class GroupBuyProgressResponse(

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
        fun from(groupBuy: GroupBuy, now: LocalDateTime = LocalDateTime.now()): GroupBuyProgressResponse {
            val achievementRate = GroupBuyProgressCalculator.achievementRate(groupBuy.currentQuantity, groupBuy.targetQuantity)
            val isClosed = GroupBuyProgressCalculator.isClosed(groupBuy, now)

            return GroupBuyProgressResponse(
                groupBuyId = groupBuy.id,
                achievementRate = achievementRate,
                currentQuantity = groupBuy.currentQuantity,
                targetQuantity = groupBuy.targetQuantity,
                isClosed = isClosed
            )
        }
    }
}
