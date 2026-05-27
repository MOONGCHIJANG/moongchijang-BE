package com.moongchijang.domain.owner.application.dto.settlement

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "사장님 월별 정산 예정 요약 응답")
data class OwnerSettlementMonthlySummaryResponse(

    @field:Schema(description = "조회 기준 연도", example = "2026")
    val year: Int,

    @field:Schema(description = "조회 기준 월(1~12)", example = "5")
    val month: Int,

    @field:Schema(description = "정산 예정 금액", example = "216000")
    val settlementExpectedAmount: Long,

    @field:Schema(description = "공구 수익금 합계", example = "240000")
    val grossRevenueAmount: Long,

    @field:Schema(description = "환불 수수료 합계", example = "24000")
    val refundFeeAmount: Long,
)
