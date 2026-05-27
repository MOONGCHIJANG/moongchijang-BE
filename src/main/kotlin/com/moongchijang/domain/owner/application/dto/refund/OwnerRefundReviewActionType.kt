package com.moongchijang.domain.owner.application.dto.refund

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "환불 요청 검토 액션")
enum class OwnerRefundReviewActionType {
    APPROVE,
    DISPUTE,
}
