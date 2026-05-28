package com.moongchijang.domain.admin.application.dto.refund

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
)

internal fun calculateSlaRemainingHours(
    requestedAt: LocalDateTime,
    now: LocalDateTime,
): Long {
    val deadline = requestedAt.plusHours(24)
    return Duration.between(now, deadline).toHours()
}
