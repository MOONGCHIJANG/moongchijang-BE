package com.moongchijang.domain.owner.application.dto.settlement

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "사장님 공구 정산 상태")
enum class OwnerSettlementStatus {
    @Schema(description = "정산 완료")
    SETTLEMENT_COMPLETED,

    @Schema(description = "정산 대기")
    SETTLEMENT_PENDING,

    @Schema(description = "환불 처리")
    REFUND_PROCESSING,
}
