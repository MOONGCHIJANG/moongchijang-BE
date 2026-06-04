package com.moongchijang.domain.admin.application.dto.refund

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.participation.domain.entity.OwnerRefundReviewStatus
import com.moongchijang.domain.participation.domain.entity.Participation
import com.moongchijang.domain.participation.domain.entity.ParticipationCancelReason
import com.moongchijang.domain.participation.domain.entity.ParticipationStatus
import com.moongchijang.domain.payment.domain.entity.Payment
import com.moongchijang.domain.payment.domain.entity.PaymentOrder
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Schema(description = "어드민 환불 요청 상세 응답")
data class AdminRefundRequestDetailResponse(

    @Schema(description = "환불 요청 ID(참여 ID)")
    val requestId: Long,

    @Schema(description = "처리 상태")
    val status: AdminRefundRequestStatus,

    @Schema(description = "SLA 남은 시간(시간)")
    val slaRemainingHours: Long,

    @Schema(description = "요청 후 1시간 초과 여부")
    val slaWarning: Boolean,

    @Schema(description = "소비자 닉네임")
    val consumerNickname: String?,

    @Schema(description = "소비자 전화번호")
    val consumerPhoneNumber: String?,

    @Schema(description = "소비자 이메일")
    val consumerEmail: String?,

    @Schema(description = "가입 방식")
    val signupProvider: String,

    @Schema(description = "공구명")
    val groupBuyName: String,

    @Schema(description = "매장명")
    val storeName: String,

    @Schema(description = "달성 여부")
    val achieved: Boolean,

    @Schema(description = "픽업일(YYYY-MM-DD)")
    val pickupDate: String,

    @Schema(description = "픽업 장소")
    val pickupLocation: String,

    @Schema(description = "결제 금액")
    val paymentAmount: Int,

    @Schema(description = "환불 예정 금액")
    val refundExpectedAmount: Int,

    @Schema(description = "결제 수단")
    val paymentMethod: String?,

    @Schema(description = "승인 번호")
    val approvalNumber: String?,

    @Schema(description = "결제 일시")
    val paidAt: LocalDateTime?,

    @Schema(description = "환불 사유")
    val refundReason: String,

    @Schema(description = "환불 상세 설명")
    val refundReasonDetail: String?,

    @Schema(description = "요청 일시")
    val requestedAt: LocalDateTime,

    @Schema(description = "사장님 의견 제출 일시")
    val ownerOpinionSubmittedAt: LocalDateTime?,

    @Schema(description = "사장님 의견 내용")
    val ownerOpinion: String?,

    @Schema(description = "처리 이력")
    val histories: List<AdminRefundRequestHistoryItemResponse>,
) {
    companion object {
        fun from(
            participation: Participation,
            paymentOrder: PaymentOrder?,
            payment: Payment?,
            now: LocalDateTime,
            consumerEmail: String?,
            consumerPhoneNumber: String?,
        ): AdminRefundRequestDetailResponse {
            val requestedAt = participation.cancelledAt ?: participation.createdAt ?: now
            return AdminRefundRequestDetailResponse(
                requestId = participation.id,
                status = participation.toAdminStatus(),
                slaRemainingHours = calculateSlaRemainingHours(requestedAt, now),
                slaWarning = isSlaWarning(requestedAt, now),
                consumerNickname = participation.user.nickname,
                consumerPhoneNumber = consumerPhoneNumber,
                consumerEmail = consumerEmail,
                signupProvider = participation.user.provider.name,
                groupBuyName = participation.groupBuy.productName,
                storeName = participation.groupBuy.store.name,
                achieved = participation.groupBuy.status == GroupBuyStatus.ACHIEVED,
                pickupDate = participation.groupBuy.pickupDate.format(PICKUP_DATE_FORMATTER),
                pickupLocation = participation.groupBuy.pickupLocation,
                paymentAmount = participation.totalAmount,
                refundExpectedAmount = participation.approvedRefundAmount
                    ?: (participation.totalAmount - participation.feeAmount.coerceAtLeast(0)).coerceAtLeast(0),
                paymentMethod = payment?.method,
                approvalNumber = payment?.pgPaymentId,
                paidAt = payment?.approvedAt ?: paymentOrder?.approvedAt,
                refundReason = participation.cancelReason.toRefundReasonLabel(),
                refundReasonDetail = participation.cancelReasonDetail,
                requestedAt = requestedAt,
                ownerOpinionSubmittedAt = participation.ownerRefundReviewedAt,
                ownerOpinion = participation.ownerRefundDisputeReason,
                histories = participation.toHistories(now),
            )
        }
    }
}

private fun Participation.toHistories(now: LocalDateTime): List<AdminRefundRequestHistoryItemResponse> {
    val requestedAt = cancelledAt ?: createdAt ?: now
    val items = mutableListOf(
        AdminRefundRequestHistoryItemResponse(
            type = "REQUESTED",
            occurredAt = requestedAt,
            memo = cancelReasonDetail,
        )
    )
    ownerRefundReviewedAt?.let {
        items.add(
            AdminRefundRequestHistoryItemResponse(
                type = "OWNER_REVIEWED",
                occurredAt = it,
                memo = ownerRefundDisputeReason,
            )
        )
    }
    refundedAt?.let {
        items.add(
            AdminRefundRequestHistoryItemResponse(
                type = "REFUNDED",
                occurredAt = it,
                memo = null,
            )
        )
    }
    return items.sortedBy { it.occurredAt }
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

private fun ParticipationCancelReason?.toRefundReasonLabel(): String {
    return when (this) {
        ParticipationCancelReason.TIME_UNAVAILABLE -> "픽업 불가"
        ParticipationCancelReason.NO_LONGER_WANTED -> "구매 의사 없음"
        ParticipationCancelReason.PREFER_DIRECT_VISIT -> "매장 직접 구매"
        ParticipationCancelReason.BOUGHT_ELSEWHERE -> "타 채널 구매"
        ParticipationCancelReason.OTHER -> "기타"
        null -> "기타"
    }
}

private val PICKUP_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
