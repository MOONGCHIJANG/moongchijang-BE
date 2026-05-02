package com.moongchijang.domain.auth.application.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "전화번호 인증코드 확인 응답")
data class PhoneVerificationVerifiedResponse(

    @field:Schema(description = "전화번호 인증 완료 여부", example = "true")
    val verified: Boolean,
)
