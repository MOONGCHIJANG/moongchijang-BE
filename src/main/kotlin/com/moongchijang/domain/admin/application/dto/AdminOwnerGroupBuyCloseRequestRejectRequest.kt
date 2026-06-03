package com.moongchijang.domain.admin.application.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "운영자 사장님 공구 마감 요청 반려 요청")
data class AdminOwnerGroupBuyCloseRequestRejectRequest(
    @field:NotBlank(message = "반려 사유는 필수입니다")
    @field:Size(max = 200, message = "반려 사유는 200자 이하이어야 합니다")
    @field:Schema(description = "반려 사유 (최대 200자)", example = "증빙 정보가 부족해 반려합니다.")
    val rejectionReason: String
)
