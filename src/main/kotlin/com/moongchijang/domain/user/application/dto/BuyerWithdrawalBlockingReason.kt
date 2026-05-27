package com.moongchijang.domain.user.application.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "소비자 탈퇴 차단 사유")
enum class BuyerWithdrawalBlockingReason {
    @Schema(description = "차단 사유 없음")
    NONE,
    @Schema(description = "수령 예정 공구 존재")
    PENDING_PICKUP,
}
