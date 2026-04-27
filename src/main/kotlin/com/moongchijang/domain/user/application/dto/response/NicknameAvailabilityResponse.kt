package com.moongchijang.domain.user.application.dto.response

data class NicknameAvailabilityResponse(
    val nickname: String,
    val available: Boolean,
)

