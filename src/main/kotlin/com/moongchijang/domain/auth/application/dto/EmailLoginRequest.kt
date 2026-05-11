package com.moongchijang.domain.auth.application.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

@Schema(description = "이메일 로그인 요청")
data class EmailLoginRequest(

    @field:NotBlank(message = "이메일은 필수입니다.")
    @field:Email(message = "올바른 이메일 형식이 아니에요.")
    @field:Schema(description = "이메일", example = "user@example.com")
    val email: String,

    @field:NotBlank(message = "비밀번호는 필수입니다.")
    @field:Schema(description = "비밀번호", example = "abc12345")
    val password: String,
)
