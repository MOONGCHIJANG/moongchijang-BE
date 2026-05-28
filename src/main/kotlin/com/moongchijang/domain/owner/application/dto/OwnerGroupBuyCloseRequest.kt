package com.moongchijang.domain.owner.application.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

@Schema(description = "사장님 공구 마감 요청")
data class OwnerGroupBuyCloseRequest(
    @field:NotNull(message = "마감 사유는 필수입니다")
    @field:Schema(description = "마감 사유", example = "STORE_CONDITION")
    val reason: OwnerGroupBuyCloseReasonType,

    @field:Size(max = 100, message = "기타 사유는 100자 이하여야 합니다")
    @field:Schema(description = "기타 사유 (최대 100자)", example = "재고 수급 이슈로 조기 마감합니다.", nullable = true)
    val reasonDetail: String? = null
)
