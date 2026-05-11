package com.moongchijang.domain.groupbuy.application.dto

import jakarta.validation.constraints.NotBlank

data class CreateGroupBuyOpenRequestRequest(
    @field:NotBlank val region: String,
    @field:NotBlank val productName: String
)
