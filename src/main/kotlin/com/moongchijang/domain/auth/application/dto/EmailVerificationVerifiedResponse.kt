package com.moongchijang.domain.auth.application.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "이메일 인증코드 확인 응답")
data class EmailVerificationVerifiedResponse(

    @field:Schema(description = "이메일 인증 완료 여부", example = "true")
    val verified: Boolean,

    @field:Schema(description = "이메일 회원가입 단계 진행용 임시 토큰", example = "eyJhbGciOiJIUzI1NiJ9...")
    val signupToken: String,
)
