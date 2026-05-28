package com.moongchijang.domain.user.application.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "이메일 중복 확인 응답")
data class EmailAvailabilityResponse(

    @field:Schema(description = "확인한 이메일", example = "user@example.com")
    val email: String,

    @field:Schema(description = "사용 가능 여부", example = "true")
    val available: Boolean,
)
