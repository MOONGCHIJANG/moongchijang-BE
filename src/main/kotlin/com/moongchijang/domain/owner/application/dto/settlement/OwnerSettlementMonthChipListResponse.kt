package com.moongchijang.domain.owner.application.dto.settlement

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "사장님 정산 조회용 연월 칩 목록 응답")
data class OwnerSettlementMonthChipListResponse(

    @field:Schema(description = "칩 목록")
    val chips: List<OwnerSettlementMonthChipResponse>,
)
