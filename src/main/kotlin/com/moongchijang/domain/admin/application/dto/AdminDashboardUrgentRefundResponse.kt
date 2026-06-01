package com.moongchijang.domain.admin.application.dto

import com.moongchijang.domain.admin.application.dto.refund.AdminRefundRequestCaseFilter
import com.moongchijang.domain.participation.domain.entity.Participation
import com.moongchijang.domain.participation.domain.entity.ParticipationCancelReason
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Duration
import java.time.LocalDateTime

@Schema(description = "대시보드 긴급 환불 요청 목록 응답")
data class AdminDashboardUrgentRefundResponse(
    @Schema(description = "1시간 초과 환불 요청 건수")
    val totalUrgentCount: Long,

    @Schema(description = "1시간 초과 환불 요청 존재 여부")
    val hasUrgentRefunds: Boolean,

    @Schema(description = "목록")
    val content: List<AdminDashboardUrgentRefundItemResponse>,

    @Schema(description = "전체 건수")
    val totalElements: Long,

    @Schema(description = "전체 페이지 수")
    val totalPages: Int,

    @Schema(description = "현재 페이지 번호(0-base)")
    val number: Int,

    @Schema(description = "페이지 크기")
    val size: Int,
)

@Schema(description = "대시보드 긴급 환불 요청 목록 아이템")
data class AdminDashboardUrgentRefundItemResponse(
    @Schema(description = "환불 요청 ID(참여 ID)")
    val requestId: Long,

    @Schema(description = "환불 케이스")
    val caseFilter: AdminRefundRequestCaseFilter,

    @Schema(description = "소비자 이름")
    val consumerName: String,

    @Schema(description = "공구명")
    val groupBuyName: String,

    @Schema(description = "환불 금액")
    val refundAmount: Int,

    @Schema(description = "요청 일시")
    val requestedAt: LocalDateTime,

    @Schema(description = "요청 후 경과 시간(시간)")
    val slaElapsedHours: Long,
) {
    companion object {
        fun from(participation: Participation, now: LocalDateTime): AdminDashboardUrgentRefundItemResponse {
            val requestedAt = participation.cancelledAt ?: participation.createdAt ?: now
            return AdminDashboardUrgentRefundItemResponse(
                requestId = participation.id,
                caseFilter = participation.toDashboardRefundCase(),
                consumerName = participation.user.nickname ?: "알 수 없음",
                groupBuyName = participation.groupBuy.productName,
                refundAmount = participation.expectedRefundAmount(),
                requestedAt = requestedAt,
                slaElapsedHours = Duration.between(requestedAt, now).toHours().coerceAtLeast(0),
            )
        }
    }
}

private fun Participation.expectedRefundAmount(): Int {
    approvedRefundAmount?.let { return it }
    return if (cancelReason == null) {
        totalAmount
    } else {
        (totalAmount - feeAmount.coerceAtLeast(0)).coerceAtLeast(0)
    }
}

private fun Participation.toDashboardRefundCase(): AdminRefundRequestCaseFilter {
    val reason = cancelReason ?: run {
        return if (groupBuy.status == GroupBuyStatus.FAILED) {
            AdminRefundRequestCaseFilter.TARGET_NOT_MET
        } else {
            AdminRefundRequestCaseFilter.OWNER_FAULT_CANCEL
        }
    }

    return when (reason) {
        ParticipationCancelReason.TIME_UNAVAILABLE -> AdminRefundRequestCaseFilter.PICKUP_PERIOD_NO_SHOW
        ParticipationCancelReason.NO_LONGER_WANTED -> AdminRefundRequestCaseFilter.PRE_ACHIEVEMENT_FREE_CANCEL
        ParticipationCancelReason.PREFER_DIRECT_VISIT,
        ParticipationCancelReason.BOUGHT_ELSEWHERE -> AdminRefundRequestCaseFilter.POST_ACHIEVEMENT_CANCEL
        ParticipationCancelReason.OTHER -> AdminRefundRequestCaseFilter.DISPUTE_OR_DROPOUT_REFUND
    }
}
