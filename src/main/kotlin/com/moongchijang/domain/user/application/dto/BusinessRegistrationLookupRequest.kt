package com.moongchijang.domain.user.application.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

@Schema(description = "사업자등록번호 조회 요청")
data class BusinessRegistrationLookupRequest(

    @field:NotBlank(message = "사업자등록번호는 필수입니다.")
    @field:Pattern(
        regexp = "^\\d{3}-?\\d{2}-?\\d{5}$",
        message = "올바른 사업자등록번호를 입력해주세요.",
    )
    @field:Schema(description = "사업자등록번호", example = "111-22-33333")
    val businessRegistrationNumber: String,
)
