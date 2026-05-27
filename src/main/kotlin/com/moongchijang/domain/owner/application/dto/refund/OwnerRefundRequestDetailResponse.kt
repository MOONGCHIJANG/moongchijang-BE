package com.moongchijang.domain.owner.application.dto.refund

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "사장님 환불 요청 상세 응답")
data class OwnerRefundRequestDetailResponse(

    @field:Schema(description = "참여 ID", example = "1001")
    val participationId: Long,

    @field:Schema(description = "공구 ID", example = "901001")
    val groupBuyId: Long,

    @field:Schema(description = "공구명", example = "초코 크루아상&소금빵 세트")
    val productName: String,

    @field:Schema(description = "요청자 이름", example = "김민지")
    val requesterName: String,

    @field:Schema(description = "요청일", example = "2026-04-29")
    val requestedDate: LocalDate,

    @field:Schema(description = "결제 금액", example = "24000")
    val paymentAmount: Int,

    @field:Schema(description = "위약금 차감액", example = "2400")
    val penaltyAmount: Int,

    @field:Schema(description = "환불 예정 금액", example = "21600")
    val refundExpectedAmount: Int,

    @field:Schema(description = "환불 사유", example = "일정 변경으로 픽업이 어려워졌어요. 환불 부탁드립니다")
    val refundReasonDetail: String?,

    @field:Schema(description = "처리 상태", example = "PENDING")
    val status: OwnerRefundRequestStatus,
)
