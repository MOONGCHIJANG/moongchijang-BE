package com.moongchijang.domain.participation.application.dto

import com.moongchijang.domain.participation.domain.entity.Participation
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "마이페이지 픽업 완료 탭 참여 카드 아이템")
data class PickupCompletedParticipationItemResponse(

    @field:Schema(description = "참여 ID", example = "1201")
    val participationId: Long,

    @field:Schema(description = "공구 ID", example = "101")
    val groupBuyId: Long,

    @field:Schema(description = "상품명", example = "버터떡 플레인 5개입")
    val productName: String,

    @field:Schema(description = "매장명", example = "모모왕국")
    val storeName: String,

    @field:Schema(description = "픽업 일시", example = "2026-04-15T14:00:00")
    val pickupAt: LocalDateTime,

    @field:Schema(description = "픽업 완료 일시", example = "2026-04-15T14:10:00")
    val pickedUpAt: LocalDateTime?,

    @field:Schema(description = "결제 금액", example = "18000")
    val paidAmount: Int,

    @field:Schema(description = "수량", example = "1")
    val quantity: Int,

    @field:Schema(description = "참여 일시", example = "2026-04-12T10:30:00")
    val participatedAt: LocalDateTime,
) {
    companion object {
        fun from(participation: Participation): PickupCompletedParticipationItemResponse {
            val groupBuy = participation.groupBuy

            return PickupCompletedParticipationItemResponse(
                participationId = participation.id,
                groupBuyId = groupBuy.id,
                productName = groupBuy.productName,
                storeName = groupBuy.store.name,
                pickupAt = LocalDateTime.of(groupBuy.pickupDate, groupBuy.pickupTimeStart),
                pickedUpAt = participation.pickedUpAt,
                paidAmount = participation.totalAmount,
                quantity = participation.quantity,
                participatedAt = requireNotNull(participation.createdAt) {
                    "[PickupCompletedParticipationItemResponse] 참여 생성일시 누락: participationId=${participation.id}"
                },
            )
        }
    }
}
