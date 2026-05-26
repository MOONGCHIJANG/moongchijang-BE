package com.moongchijang.domain.owner.application.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "사장님 홈 공구 요약 응답")
data class OwnerGroupBuySummaryResponse(

    @field:Schema(description = "진행 중 공구 건수", example = "3")
    val ongoingCount: Int,

    @field:Schema(description = "달성 완료 공구 건수", example = "2")
    val achievedCount: Int,

    @field:Schema(description = "오늘 픽업 예정 인원 수", example = "14")
    val todayPickupUserCount: Int,

    @field:Schema(description = "정산 예정 금액", example = "128000")
    val settlementExpectedAmount: Int,

    @field:Schema(description = "공구 요약 데이터 비어있는지 여부", example = "false")
    val isEmpty: Boolean
)
