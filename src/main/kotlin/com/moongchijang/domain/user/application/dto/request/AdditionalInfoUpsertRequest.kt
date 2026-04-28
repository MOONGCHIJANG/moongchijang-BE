package com.moongchijang.domain.user.application.dto.request

import jakarta.validation.constraints.NotBlank

data class AdditionalInfoUpsertRequest(
    @field:NotBlank
    val nickname: String,
    @field:NotBlank
    val phoneNumber: String,
)

