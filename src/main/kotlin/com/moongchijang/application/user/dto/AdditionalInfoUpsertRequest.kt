package com.moongchijang.application.user.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

@Schema(description = "신규 가입자 추가정보 입력 요청")
data class AdditionalInfoUpsertRequest(

    @field:NotBlank(message = "닉네임은 필수입니다.")
    @field:Size(min = 2, max = 10, message = "닉네임은 2자 이상 10자 이하여야 합니다.")
    @field:Pattern(
        regexp = "^[가-힣a-zA-Z0-9]{2,10}$",
        message = "2~10자, 한글/영문/숫자만 입력 가능해요.",
    )
    @field:Schema(description = "닉네임", example = "문치장")
    val nickname: String,

    @field:NotBlank(message = "전화번호는 필수입니다.")
    @field:Pattern(
        regexp = "^01[0-9]-?\\d{3,4}-?\\d{4}$",
        message = "올바른 전화번호를 입력해주세요.",
    )
    @field:Schema(description = "전화번호", example = "010-1234-5678")
    val phoneNumber: String,
)
