package com.moongchijang.application.user.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "닉네임 중복 확인 응답")
data class NicknameAvailabilityResponse(

    @field:Schema(description = "확인한 닉네임", example = "문치장")
    val nickname: String,

    @field:Schema(description = "사용 가능 여부", example = "true")
    val available: Boolean,
)
