package com.moongchijang.application.auth.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

@Schema(description = "전화번호 인증코드 발송 요청")
data class PhoneVerificationCodeSendRequest(

    @field:NotBlank
    @field:Pattern(
        regexp = "^01[0-9]-?\\d{3,4}-?\\d{4}$",
        message = "올바른 전화번호를 입력해주세요."
    )
    @field:Schema(description= "전화번호", example = "010-1234-5678")
    val phoneNumber: String,
)
