package com.moongchijang.domain.user.application.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "사장님 탈퇴 차단 사유")
enum class SellerWithdrawalBlockingReason {
    @Schema(description = "차단 사유 없음")
    NONE,
    @Schema(description = "진행중 개설 공구 존재")
    OPEN_GROUPBUY,
    @Schema(description = "고객 미픽업 존재")
    PENDING_CUSTOMER_PICKUP,
}
