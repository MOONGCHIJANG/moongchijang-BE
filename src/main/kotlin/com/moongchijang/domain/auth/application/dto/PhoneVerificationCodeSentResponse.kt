package com.moongchijang.domain.auth.application.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "전화번호 인증코드 발송 응답")
data class PhoneVerificationCodeSentResponse(

    @field:Schema(description = "인증번호 만료까지 남은 시간(초)", example = "180")
    val expiresInSeconds: Int,

    @field:Schema(description = "재발송 가능까지 남은 시간(초)", example = "0")
    val resendAvailableInSeconds: Int,
)
