package com.moongchijang.domain.user.application.dto

import com.moongchijang.domain.user.domain.entity.UserRole
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "탈퇴 진입 컨텍스트 응답")
data class WithdrawalContextResponse(

    @field:Schema(description = "현재 사용자 활성 역할", example = "BUYER")
    val currentRole: UserRole,

    @field:Schema(description = "소비자 탈퇴 컨텍스트")
    val buyer: BuyerWithdrawalContext,

    @field:Schema(description = "사장님 탈퇴 컨텍스트")
    val seller: SellerWithdrawalContext,

    @field:Schema(description = "권장 탈퇴 화면 타입", example = "BUYER_WITHDRAWAL")
    val recommendedScreen: WithdrawalScreenType,

    @field:Schema(description = "현재 역할 화면에서 강제 이동 필요 여부", example = "false")
    val forceRedirect: Boolean,

    @field:Schema(description = "강제 이동 대상 화면 타입(forceRedirect=true일 때만 존재)", nullable = true, example = "SELLER_WITHDRAWAL")
    val forceRedirectTarget: WithdrawalScreenType?,
)
