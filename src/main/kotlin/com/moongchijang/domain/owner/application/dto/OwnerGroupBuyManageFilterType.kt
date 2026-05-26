package com.moongchijang.domain.owner.application.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "사장님 공구 관리 목록 필터")
enum class OwnerGroupBuyManageFilterType {
    ALL,
    IN_PROGRESS,
    ACHIEVED,
    ENDED,
    PENDING_APPROVAL
}
