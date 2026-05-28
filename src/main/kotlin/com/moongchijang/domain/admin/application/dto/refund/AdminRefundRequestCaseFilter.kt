package com.moongchijang.domain.admin.application.dto.refund

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "어드민 환불 요청 케이스 필터")
enum class AdminRefundRequestCaseFilter {
    ALL,
    PRE_ACHIEVEMENT_FREE_CANCEL,
    POST_ACHIEVEMENT_CANCEL,
    PICKUP_PERIOD_NO_SHOW,
    OWNER_FAULT_CANCEL,
    TARGET_NOT_MET,
    DISPUTE_OR_DROPOUT_REFUND,
}
