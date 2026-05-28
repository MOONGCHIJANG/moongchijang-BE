package com.moongchijang.domain.admin.application.dto.refund

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min

@Schema(description = "어드민 환불 요청 승인 요청")
data class AdminRefundRequestApproveRequest(
    @field:Schema(description = "환불 승인 금액", example = "12000")
    @field:Min(0)
    val refundAmount: Int,
)
