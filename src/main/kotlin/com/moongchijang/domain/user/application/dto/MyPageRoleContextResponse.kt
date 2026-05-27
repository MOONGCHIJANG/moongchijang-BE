package com.moongchijang.domain.user.application.dto

import com.moongchijang.domain.user.domain.entity.UserRole
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "마이페이지 역할 컨텍스트 응답")
data class MyPageRoleContextResponse(

    @field:Schema(description = "현재 활성 역할", example = "BUYER")
    val currentRole: UserRole,

    @field:Schema(description = "마지막 사용 역할", example = "SELLER")
    val lastRole: UserRole?,

    @field:Schema(description = "소비자 역할 보유 여부", example = "true")
    val hasBuyerRole: Boolean,

    @field:Schema(description = "사장님 역할 보유 여부", example = "true")
    val hasSellerRole: Boolean,

    @field:Schema(description = "소비자 모드 전환 가능 여부", example = "true")
    val canSwitchToBuyer: Boolean,

    @field:Schema(description = "사장님 모드 전환 가능 여부", example = "true")
    val canSwitchToSeller: Boolean,
)
