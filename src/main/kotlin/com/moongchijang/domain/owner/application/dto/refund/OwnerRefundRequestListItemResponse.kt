package com.moongchijang.domain.owner.application.dto.refund

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "사장님 환불 요청 목록 아이템")
data class OwnerRefundRequestListItemResponse(

    @field:Schema(description = "참여 ID", example = "1001")
    val participationId: Long,

    @field:Schema(description = "공구 ID", example = "901001")
    val groupBuyId: Long,

    @field:Schema(description = "공구명", example = "초코 크루아상&소금빵 세트")
    val productName: String,

    @field:Schema(description = "결제 금액", example = "24000")
    val paymentAmount: Int,

    @field:Schema(description = "요청자 이름", example = "김민지")
    val requesterName: String,

    @field:Schema(description = "요청자 표기 코드", example = "P001")
    val requesterCode: String,

    @field:Schema(description = "환불 사유 라벨", example = "픽업 불가")
    val refundReasonLabel: String,

    @field:Schema(description = "요청일", example = "2026-05-02")
    val requestedDate: LocalDate,

    @field:Schema(description = "처리 상태", example = "PENDING")
    val status: OwnerRefundRequestStatus,

    @field:Schema(description = "검토 대기 24시간 초과 여부", example = "true")
    val exceeded24Hours: Boolean,
)
