package com.moongchijang.domain.participation.application.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "마이페이지 탭별 건수 응답")
data class MyPageTabCountsResponse(

    @field:Schema(description = "진행 중 참여 건수", example = "4")
    val inProgressCount: Long,

    @field:Schema(description = "픽업 대기 참여 건수", example = "2")
    val pickupWaitingCount: Long,

    @field:Schema(description = "픽업 완료 참여 건수", example = "1")
    val pickupCompletedCount: Long,

    @field:Schema(description = "환불/취소 참여 건수", example = "1")
    val cancelledOrRefundedCount: Long,

    @field:Schema(description = "공구 개설 요청 건수", example = "3")
    val requestCount: Long,
)
