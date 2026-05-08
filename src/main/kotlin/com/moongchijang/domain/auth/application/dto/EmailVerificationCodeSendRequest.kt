package com.moongchijang.domain.auth.application.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

@Schema(description = "이메일 인증코드 발송 요청")
data class EmailVerificationCodeSendRequest(

    @field:NotBlank(message = "이메일은 필수입니다.")
    @field:Email(message = "올바른 이메일 형식이 아니에요.")
    @field:Schema(description = "이메일", example = "user@example.com")
    val email: String,
)
