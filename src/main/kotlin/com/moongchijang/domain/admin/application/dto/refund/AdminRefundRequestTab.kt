package com.moongchijang.domain.admin.application.dto.refund

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "어드민 환불 요청 탭 필터")
enum class AdminRefundRequestTab {
    ALL,
    REVIEW_PENDING,
    IN_PROGRESS,
    APPROVED,
    REJECTED,
}
