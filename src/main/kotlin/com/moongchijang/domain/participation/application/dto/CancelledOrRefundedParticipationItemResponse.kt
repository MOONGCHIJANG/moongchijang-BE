package com.moongchijang.domain.participation.application.dto

import com.moongchijang.domain.participation.domain.entity.Participation
import com.moongchijang.domain.participation.domain.entity.ParticipationCancelReason
import com.moongchijang.domain.participation.domain.entity.ParticipationStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "마이페이지 환불/취소 탭 참여 카드 아이템")
data class CancelledOrRefundedParticipationItemResponse(

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

    @field:Schema(description = "결제 금액", example = "18000")
    val paidAmount: Int,

    @field:Schema(description = "수량", example = "1")
    val quantity: Int,

    @field:Schema(description = "참여 상태", example = "REFUNDED")
    val status: ParticipationStatus,

    @field:Schema(description = "취소 사유", nullable = true)
    val cancelReason: ParticipationCancelReason?,

    @field:Schema(description = "취소 사유 상세", nullable = true)
    val cancelReasonDetail: String?,

    @field:Schema(description = "취소 일시", nullable = true)
    val cancelledAt: LocalDateTime?,

    @field:Schema(description = "환불 일시", nullable = true)
    val refundedAt: LocalDateTime?,

    @field:Schema(description = "참여 일시", example = "2026-04-12T10:30:00")
    val participatedAt: LocalDateTime,
) {
    companion object {
        fun from(participation: Participation): CancelledOrRefundedParticipationItemResponse {
            val groupBuy = participation.groupBuy

            return CancelledOrRefundedParticipationItemResponse(
                participationId = participation.id,
                groupBuyId = groupBuy.id,
                productName = groupBuy.productName,
                storeName = groupBuy.store.name,
                pickupAt = LocalDateTime.of(groupBuy.pickupDate, groupBuy.pickupTimeStart),
                paidAmount = participation.totalAmount,
                quantity = participation.quantity,
                status = participation.status,
                cancelReason = participation.cancelReason,
                cancelReasonDetail = participation.cancelReasonDetail,
                cancelledAt = participation.cancelledAt,
                refundedAt = participation.refundedAt,
                participatedAt = requireNotNull(participation.createdAt) {
                    "[CancelledOrRefundedParticipationItemResponse] 참여 생성일시 누락: participationId=${participation.id}"
                },
            )
        }
    }
}
