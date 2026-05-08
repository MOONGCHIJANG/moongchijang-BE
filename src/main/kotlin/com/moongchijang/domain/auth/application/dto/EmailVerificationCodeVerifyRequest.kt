package com.moongchijang.domain.auth.application.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

@Schema(description = "이메일 인증코드 확인 요청")
data class EmailVerificationCodeVerifyRequest(

    @field:NotBlank(message = "이메일은 필수입니다.")
    @field:Email(message = "올바른 이메일 형식이 아니에요.")
    @field:Schema(description = "이메일", example = "user@example.com")
    val email: String,

    @field:NotBlank(message = "인증코드는 필수입니다.")
    @field:Pattern(
        regexp = "^[0-9]{6}$",
        message = "인증코드는 6자리 숫자여야 합니다.",
    )
    @field:Schema(description = "6자리 인증코드", example = "123456")
    val code: String,
)
