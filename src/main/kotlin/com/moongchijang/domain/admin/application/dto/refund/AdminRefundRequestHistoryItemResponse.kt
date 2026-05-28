package com.moongchijang.domain.admin.application.dto.refund

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "어드민 환불 요청 처리 이력 아이템")
data class AdminRefundRequestHistoryItemResponse(

    @Schema(description = "이력 타입")
    val type: String,

    @Schema(description = "이력 시각")
    val occurredAt: LocalDateTime,

    @Schema(description = "이력 메모")
    val memo: String?,
)
