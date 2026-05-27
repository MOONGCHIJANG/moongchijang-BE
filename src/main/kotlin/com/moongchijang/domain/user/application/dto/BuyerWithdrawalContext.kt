package com.moongchijang.domain.user.application.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "소비자 탈퇴 컨텍스트")
data class BuyerWithdrawalContext(

    @field:Schema(description = "소비자 탈퇴 진행 가능 여부", example = "true")
    val canProceed: Boolean,

    @field:Schema(description = "수령 예정 공구 존재 여부", example = "false")
    val hasPendingPickup: Boolean,

    @field:Schema(description = "참여 중 공구(PAID_WAITING_GOAL) 존재 여부", example = "true")
    val hasActiveParticipation: Boolean,

    @field:Schema(description = "소비자 탈퇴 차단 사유", example = "NONE")
    val blockingReason: BuyerWithdrawalBlockingReason,
)
