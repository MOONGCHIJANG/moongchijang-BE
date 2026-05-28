package com.moongchijang.domain.groupbuy.application.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateGroupBuyOpenRequestRequest(
    @field:NotBlank(message = "지역은 필수입니다")
    @field:Size(max = 50, message = "지역은 50자 이하이어야 합니다")
    val region: String,

    @field:NotBlank(message = "상품명은 필수입니다")
    @field:Size(max = 100, message = "상품명은 100자 이하이어야 합니다")
    val productName: String
)
