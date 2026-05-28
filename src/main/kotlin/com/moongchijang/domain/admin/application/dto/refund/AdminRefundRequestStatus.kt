package com.moongchijang.domain.admin.application.dto.refund

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "어드민 환불 요청 처리 상태")
enum class AdminRefundRequestStatus {
    REVIEW_PENDING,
    IN_PROGRESS,
    APPROVED,
    REJECTED,
}
