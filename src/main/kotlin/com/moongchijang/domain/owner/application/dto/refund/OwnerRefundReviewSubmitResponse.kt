package com.moongchijang.domain.owner.application.dto.refund

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "사장님 환불 요청 검토 제출 응답")
data class OwnerRefundReviewSubmitResponse(

    @field:Schema(description = "참여 ID", example = "1001")
    val participationId: Long,

    @field:Schema(description = "처리 완료 여부", example = "true")
    val processed: Boolean,
)
