package com.moongchijang.domain.owner.application.dto.refund

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "사장님 환불 요청 목록 응답")
data class OwnerRefundRequestListResponse(

    @field:Schema(description = "검토 대기 건수", example = "1")
    val pendingCount: Int,

    @field:Schema(description = "처리 완료 건수", example = "2")
    val completedCount: Int,

    @field:Schema(description = "검토 대기 항목 존재 여부", example = "true")
    val hasPendingItems: Boolean,

    @field:Schema(description = "목록 아이템")
    val items: List<OwnerRefundRequestListItemResponse>,
)
