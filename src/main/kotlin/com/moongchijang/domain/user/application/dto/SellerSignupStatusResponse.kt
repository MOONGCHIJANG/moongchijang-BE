package com.moongchijang.domain.user.application.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "사장님 가입 상태 응답")
data class SellerSignupStatusResponse(

    @field:Schema(description = "사용자 ID", example = "1")
    val id: Long,

    @field:Schema(description = "사장님 가입 완료 여부", example = "true")
    val sellerSignupCompleted: Boolean,
)
