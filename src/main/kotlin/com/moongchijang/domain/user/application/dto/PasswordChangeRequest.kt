package com.moongchijang.domain.user.application.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

@Schema(description = "비밀번호 변경 요청")
data class PasswordChangeRequest(

    @field:NotBlank(message = "현재 비밀번호는 필수입니다.")
    @field:Schema(description = "현재 비밀번호")
    val currentPassword: String,

    @field:NotBlank(message = "새 비밀번호는 필수입니다.")
    @field:Size(min = 8, max = 20, message = "8자 이상, 영문+숫자를 포함해주세요.")
    @field:Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*[0-9]).{8,20}$",
        message = "8자 이상, 영문+숫자를 포함해주세요.",
    )
    @field:Schema(
        description = "새 비밀번호 (8~20자, 영문+숫자 포함, 이메일 아이디와 동일값 불가)",
        example = "abc12345",
    )
    val newPassword: String,
)
