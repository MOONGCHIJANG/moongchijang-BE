package com.moongchijang.domain.owner.application.dto.settlement

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "사장님 정산 공구 카드 목록 응답")
data class OwnerSettlementItemListResponse(

    @field:Schema(description = "조회 기준 연도", example = "2026")
    val year: Int,

    @field:Schema(description = "조회 기준 월(1~12)", example = "5")
    val month: Int,

    @field:Schema(description = "정산 공구 카드 목록")
    val items: List<OwnerSettlementItemResponse>,
)
