package com.moongchijang.domain.auth.application.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

@Schema(description = "이메일 회원가입 요청")
data class EmailSignupRequest(

    @field:NotBlank(message = "이메일은 필수입니다.")
    @field:Email(message = "올바른 이메일 형식이 아니에요.")
    @field:Schema(description = "이메일", example = "user@example.com")
    val email: String,

    @field:NotBlank(message = "비밀번호는 필수입니다.")
    @field:Size(min = 8, max = 20, message = "8자 이상, 영문+숫자를 포함해주세요.")
    @field:Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*[0-9]).{8,20}$",
        message = "8자 이상, 영문+숫자를 포함해주세요.",
    )
    @field:Schema(
        description = "비밀번호 (8~20자, 영문+숫자 포함, 이메일 아이디와 동일값 불가)",
        example = "abc12345",
    )
    val password: String,

    @field:NotBlank(message = "회원가입 토큰은 필수입니다.")
    @field:Schema(description = "이메일 인증 완료 후 발급되는 회원가입 진행 토큰")
    val signupToken: String,
)
