package com.moongchijang.domain.admin.application.dto.refund

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "어드민 환불 요청 목록 페이지 응답")
data class AdminRefundRequestPageResponse(

    @Schema(description = "목록")
    val content: List<AdminRefundRequestListItemResponse>,

    @Schema(description = "전체 건수")
    val totalElements: Long,

    @Schema(description = "전체 페이지 수")
    val totalPages: Int,

    @Schema(description = "현재 페이지 번호(0-base)")
    val number: Int,

    @Schema(description = "페이지 크기")
    val size: Int,

    @Schema(description = "요청 후 1시간 초과 SLA 경고 건 존재 여부")
    val hasSlaWarning: Boolean,

    @Schema(description = "요청 후 1시간 초과 SLA 경고 건수")
    val slaWarningCount: Int,
)
