package com.moongchijang.domain.admin.application.dto.refund

import com.moongchijang.domain.participation.domain.entity.OwnerRefundReviewStatus
import com.moongchijang.domain.participation.domain.entity.Participation
import com.moongchijang.domain.participation.domain.entity.ParticipationCancelReason
import com.moongchijang.domain.participation.domain.entity.ParticipationStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Duration
import java.time.LocalDateTime

@Schema(description = "어드민 환불 요청 목록 아이템")
data class AdminRefundRequestListItemResponse(

    @Schema(description = "환불 요청 ID(참여 ID)")
    val requestId: Long,

    @Schema(description = "환불 케이스")
    val caseFilter: AdminRefundRequestCaseFilter,

    @Schema(description = "소비자명")
    val consumerName: String,

    @Schema(description = "공구명")
    val groupBuyName: String,

    @Schema(description = "매장명")
    val storeName: String,

    @Schema(description = "결제 금액")
    val paymentAmount: Int,

    @Schema(description = "환불 금액")
    val refundAmount: Int,

    @Schema(description = "사장님 의견")
    val ownerOpinion: String?,

    @Schema(description = "요청 일시")
    val requestedAt: LocalDateTime,

    @Schema(description = "SLA 남은 시간(시간)")
    val slaRemainingHours: Long,

    @Schema(description = "처리 상태")
    val status: AdminRefundRequestStatus,
){
    companion object {
        fun from(
            participation: Participation,
            now: LocalDateTime,
        ): AdminRefundRequestListItemResponse {
            val requestedAt = participation.cancelledAt ?: participation.createdAt ?: now
            return AdminRefundRequestListItemResponse(
                requestId = participation.id,
                caseFilter = participation.cancelReason.toAdminCaseFilter(),
                consumerName = participation.user.nickname ?: "알 수 없음",
                groupBuyName = participation.groupBuy.productName,
                storeName = participation.groupBuy.store.name,
                paymentAmount = participation.totalAmount,
                refundAmount = if (participation.status == ParticipationStatus.REFUNDED) participation.totalAmount else 0,
                ownerOpinion = participation.ownerRefundDisputeReason,
                requestedAt = requestedAt,
                slaRemainingHours = calculateSlaRemainingHours(requestedAt, now),
                status = participation.toAdminStatus(),
            )
        }
    }
}

private fun Participation.toAdminStatus(): AdminRefundRequestStatus {
    if (status == ParticipationStatus.REFUNDED) {
        return AdminRefundRequestStatus.APPROVED
    }

    return when (ownerRefundReviewStatus) {
        OwnerRefundReviewStatus.APPROVED -> AdminRefundRequestStatus.IN_PROGRESS
        OwnerRefundReviewStatus.DISPUTED -> AdminRefundRequestStatus.REJECTED
        OwnerRefundReviewStatus.PENDING,
        null -> AdminRefundRequestStatus.REVIEW_PENDING
    }
}

private fun ParticipationCancelReason?.toAdminCaseFilter(): AdminRefundRequestCaseFilter {
    return when (this) {
        ParticipationCancelReason.TIME_UNAVAILABLE -> AdminRefundRequestCaseFilter.PICKUP_PERIOD_NO_SHOW
        ParticipationCancelReason.NO_LONGER_WANTED -> AdminRefundRequestCaseFilter.PRE_ACHIEVEMENT_FREE_CANCEL
        ParticipationCancelReason.PREFER_DIRECT_VISIT -> AdminRefundRequestCaseFilter.POST_ACHIEVEMENT_CANCEL
        ParticipationCancelReason.BOUGHT_ELSEWHERE -> AdminRefundRequestCaseFilter.POST_ACHIEVEMENT_CANCEL
        ParticipationCancelReason.OTHER -> AdminRefundRequestCaseFilter.DISPUTE_OR_DROPOUT_REFUND
        null -> AdminRefundRequestCaseFilter.ALL
    }
}

internal fun calculateSlaRemainingHours(
    requestedAt: LocalDateTime,
    now: LocalDateTime,
): Long {
    val deadline = requestedAt.plusHours(24)
    return Duration.between(now, deadline).toHours()
}
