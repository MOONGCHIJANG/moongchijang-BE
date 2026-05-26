package com.moongchijang.domain.user.application.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

@Schema(description = "전화번호 변경 요청")
data class PhoneNumberUpdateRequest(

    @field:NotBlank(message = "전화번호는 필수입니다.")
    @field:Pattern(
        regexp = "^01[0-9]-[0-9]{3,4}-[0-9]{4}$",
        message = "올바른 전화번호를 입력해주세요.",
    )
    @field:Schema(description = "변경할 전화번호", example = "010-1234-5678")
    val phoneNumber: String,
)
