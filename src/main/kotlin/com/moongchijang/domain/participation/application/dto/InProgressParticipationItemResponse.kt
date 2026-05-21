package com.moongchijang.domain.participation.application.dto

import com.moongchijang.domain.groupbuy.application.dto.GroupBuyProgressCalculator
import com.moongchijang.domain.participation.domain.entity.Participation
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Schema(description = "마이페이지 진행 중 탭 참여 카드 아이템")
data class InProgressParticipationItemResponse(

    @field:Schema(description = "참여 ID", example = "1201")
    val participationId: Long,

    @field:Schema(description = "공구 ID", example = "101")
    val groupBuyId: Long,

    @field:Schema(description = "상품명", example = "두쭌쿠 오리지널 1개")
    val productName: String,

    @field:Schema(description = "매장명", example = "사이드템포")
    val storeName: String,

    @field:Schema(description = "픽업 일시", example = "2026-04-15T14:00:00")
    val pickupAt: LocalDateTime,

    @field:Schema(description = "결제 금액", example = "36000")
    val paidAmount: Int,

    @field:Schema(description = "수량", example = "2")
    val quantity: Int,

    @field:Schema(description = "달성률(%)", example = "72")
    val achievementRate: Int,

    @field:Schema(description = "마감 D-day 숫자", example = "2")
    val dDay: Int,

    @field:Schema(description = "참여 일시", example = "2026-04-12T10:30:00")
    val participatedAt: LocalDateTime
) {
    companion object {
        fun from(participation: Participation, now: LocalDateTime = LocalDateTime.now()): InProgressParticipationItemResponse {
            val groupBuy = participation.groupBuy
            val dDay = ChronoUnit.DAYS.between(
                now.toLocalDate(),
                groupBuy.deadline.toLocalDate()
            ).toInt()

            return InProgressParticipationItemResponse(
                participationId = participation.id,
                groupBuyId = groupBuy.id,
                productName = groupBuy.productName,
                storeName = groupBuy.store.name,
                pickupAt = LocalDateTime.of(groupBuy.pickupDate, groupBuy.pickupTimeStart),
                paidAmount = participation.totalAmount,
                quantity = participation.quantity,
                achievementRate = GroupBuyProgressCalculator.achievementRate(
                    currentQuantity = groupBuy.currentQuantity,
                    targetQuantity = groupBuy.targetQuantity
                ),
                dDay = dDay,
                participatedAt = requireNotNull(participation.createdAt) {
                    "[InProgressParticipationItemResponse] 참여 생성일시 누락: participationId=${participation.id}"
                }
            )
        }
    }
}
