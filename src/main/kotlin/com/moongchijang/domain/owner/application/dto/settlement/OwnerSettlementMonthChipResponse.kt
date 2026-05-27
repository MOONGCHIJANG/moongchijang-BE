package com.moongchijang.domain.owner.application.dto.settlement

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "사장님 정산 조회용 연월 칩 응답")
data class OwnerSettlementMonthChipResponse(

    @field:Schema(description = "연도", example = "2026")
    val year: Int,

    @field:Schema(description = "월(1~12)", example = "5")
    val month: Int,

    @field:Schema(description = "표시 라벨(YYYY년 M월)", example = "2026년 5월")
    val label: String,
)
