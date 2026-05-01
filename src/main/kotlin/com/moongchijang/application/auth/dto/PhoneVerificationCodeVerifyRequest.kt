package com.moongchijang.application.auth.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

@Schema(description = "전화번호 인증코드 확인 요청")
data class PhoneVerificationCodeVerifyRequest(

    @field:NotBlank(message = "전화번호는 필수입니다.")
    @field:Pattern(
        regexp = "^01[0-9]-?\\d{3,4}-?\\d{4}$",
        message = "올바른 전화번호를 입력해주세요.",
    )
    @field:Schema(description = "전화번호", example = "010-1234-5678")
    val phoneNumber: String,

    @field:NotBlank(message = "인증번호는 필수입니다.")
    @field:Pattern(
        regexp = "^[0-9]{6}$",
        message = "인증번호는 6자리 숫자여야 합니다.",
    )
    @field:Schema(description = "6자리 인증번호", example = "123456")
    val code: String,
)
