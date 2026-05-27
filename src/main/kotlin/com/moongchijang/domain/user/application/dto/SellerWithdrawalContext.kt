package com.moongchijang.domain.user.application.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "사장님 탈퇴 컨텍스트")
data class SellerWithdrawalContext(

    @field:Schema(description = "사장님 탈퇴 진행 가능 여부", example = "false")
    val canProceed: Boolean,

    @field:Schema(description = "개설된 진행중 공구 존재 여부", example = "true")
    val hasOpenGroupBuy: Boolean,

    @field:Schema(description = "달성/완료 공구 고객 미픽업 존재 여부", example = "false")
    val hasPendingCustomerPickup: Boolean,

    @field:Schema(description = "사장님 탈퇴 차단 사유", example = "OPEN_GROUPBUY")
    val blockingReason: SellerWithdrawalBlockingReason,
)
