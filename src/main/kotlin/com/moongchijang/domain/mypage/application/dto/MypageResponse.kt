package com.moongchijang.domain.mypage.application.dto

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequest
import com.moongchijang.domain.participation.domain.entity.Participation
import com.moongchijang.domain.participation.domain.entity.ParticipationStatus
import com.moongchijang.domain.participation.domain.entity.PickupStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

data class MypageSummaryResponse(
    val activeCount: Long,
    val completedCount: Long,
    val refundedCount: Long,
    val requestCount: Long
)

data class MypageRefundResponse(
    val participationId: Long,
    val productName: String,
    val refundStatus: String,
    val storeName: String,
    val pickupDate: LocalDate,
    val pickupTimeStart: LocalTime,
    val pickupTimeEnd: LocalTime,
    val paymentAmount: Int,
    val quantity: Int,
    val cancelReason: String?,
    val cancelReasonDetail: String?,
    val refundedAt: LocalDateTime?
) {
    companion object {
        fun from(participation: Participation): MypageRefundResponse {
            val groupBuy = participation.groupBuy

            return MypageRefundResponse(
                participationId = participation.id,
                productName = groupBuy.productName,
                refundStatus = "COMPLETED",
                storeName = groupBuy.store.name,
                pickupDate = groupBuy.pickupDate,
                pickupTimeStart = groupBuy.pickupTimeStart,
                pickupTimeEnd = groupBuy.pickupTimeEnd,
                paymentAmount = participation.totalAmount,
                quantity = participation.quantity,
                cancelReason = participation.cancelReason?.name,
                cancelReasonDetail = participation.cancelReasonDetail,
                refundedAt = participation.refundedAt
            )
        }
    }
}

data class MypageParticipationResponse(
    val participationId: Long,
    val groupBuyId: Long,
    val productName: String,
    val participationStatus: String,
    val achievementRate: Int,
    val achievementStatus: String,
    val displayStatus: String,
    val storeName: String,
    val pickupDate: LocalDate,
    val pickupTimeStart: LocalTime,
    val pickupTimeEnd: LocalTime,
    val pickupLocation: String,
    val paymentAmount: Int,
    val quantity: Int,
    val pickupStatus: String,
    val dDay: Int,
    val canCancel: Boolean,
    val canViewPickup: Boolean,
    val canViewQr: Boolean,
    val qrAvailability: String
) {
    companion object {
        fun from(
            participation: Participation,
            approvedPaymentGroupBuyIds: Set<Long> = emptySet()
        ): MypageParticipationResponse {
            val groupBuy = participation.groupBuy
            val canViewPickupOrQr = participation.status == ParticipationStatus.CONFIRMED &&
                participation.pickupStatus in listOf(PickupStatus.NOT_READY, PickupStatus.READY)

            return MypageParticipationResponse(
                participationId = participation.id,
                groupBuyId = groupBuy.id,
                productName = groupBuy.productName,
                participationStatus = participation.status.name,
                achievementRate = achievementRate(groupBuy.currentQuantity, groupBuy.targetQuantity),
                achievementStatus = achievementStatus(participation.status),
                displayStatus = displayStatus(participation.status, participation.pickupStatus),
                storeName = groupBuy.store.name,
                pickupDate = groupBuy.pickupDate,
                pickupTimeStart = groupBuy.pickupTimeStart,
                pickupTimeEnd = groupBuy.pickupTimeEnd,
                pickupLocation = groupBuy.pickupLocation,
                paymentAmount = participation.totalAmount,
                quantity = participation.quantity,
                pickupStatus = participation.pickupStatus.name,
                dDay = ChronoUnit.DAYS.between(LocalDate.now(), groupBuy.deadline.toLocalDate()).toInt(),
                canCancel = participation.status == ParticipationStatus.PAID_WAITING_GOAL &&
                    groupBuy.id in approvedPaymentGroupBuyIds,
                canViewPickup = canViewPickupOrQr,
                canViewQr = canViewPickupOrQr,
                qrAvailability = qrAvailability(participation)
            )
        }

        private fun achievementRate(currentQuantity: Int, targetQuantity: Int): Int {
            if (targetQuantity <= 0) return 0
            return ((currentQuantity * 100) / targetQuantity).coerceIn(0, 100)
        }

        private fun achievementStatus(status: ParticipationStatus): String =
            when (status) {
                ParticipationStatus.CONFIRMED -> "ACHIEVED"
                else -> "BEFORE_ACHIEVED"
            }

        private fun displayStatus(status: ParticipationStatus, pickupStatus: PickupStatus): String =
            when {
                pickupStatus == PickupStatus.PICKED_UP -> "픽업 완료"
                status == ParticipationStatus.PAID_WAITING_GOAL -> "참여중 / 달성 전"
                status == ParticipationStatus.CONFIRMED -> "참여중 / 달성 완료"
                status == ParticipationStatus.CANCELLED -> "취소"
                status == ParticipationStatus.REFUNDED -> "환불 완료"
                status == ParticipationStatus.PENDING -> "결제 대기"
                else -> status.name
            }

        private fun qrAvailability(participation: Participation): String =
            when {
                participation.pickupStatus == PickupStatus.PICKED_UP -> "PICKED_UP"
                participation.status != ParticipationStatus.CONFIRMED -> "UNAVAILABLE"
                LocalDate.now().isBefore(participation.groupBuy.pickupDate) -> "LOCKED"
                else -> "AVAILABLE"
            }
    }
}

data class MypageGroupBuyRequestResponse(
    val productName: String,
    val status: String,
    val storeName: String,
    val desiredPickupDate: LocalDate,
    val desiredQuantity: Int,
    val requestId: Long
) {
    companion object {
        fun from(request: GroupBuyRequest): MypageGroupBuyRequestResponse =
            MypageGroupBuyRequestResponse(
                productName = request.productName,
                status = request.status.name,
                storeName = request.storeName,
                desiredPickupDate = request.desiredPickupDate,
                desiredQuantity = request.desiredQuantity,
                requestId = request.id
            )
    }
}
