package com.moongchijang.domain.user.application.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "탈퇴 화면 타입")
enum class WithdrawalScreenType {
    @Schema(description = "소비자 탈퇴 화면")
    BUYER_WITHDRAWAL,
    @Schema(description = "사장님 탈퇴 화면")
    SELLER_WITHDRAWAL,
}
