package com.moongchijang.domain.user.application.dto

import com.moongchijang.domain.user.domain.entity.UserRole
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(description = "마이페이지 역할 전환 요청")
data class MyPageRoleSwitchRequest(

    @field:NotNull(message = "전환할 역할은 필수입니다.")
    @field:Schema(description = "전환할 역할", example = "SELLER")
    val role: UserRole,
)
