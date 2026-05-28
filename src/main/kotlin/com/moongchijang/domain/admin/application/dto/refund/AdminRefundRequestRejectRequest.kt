package com.moongchijang.domain.admin.application.dto.refund

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "운영자 환불 요청 거절 처리 요청 DTO")
data class AdminRefundRequestRejectRequest(

    @field:NotBlank(message = "rejectionReason은 필수입니다.")
    @field:Size(max = 200, message = "rejectionReason은 200자를 초과할 수 없습니다.")
    @Schema(description = "환불 요청 거절 사유 (최대 200자)", example = "증빙 자료 불충분으로 환불이 어렵습니다.")
    val rejectionReason: String,
)
