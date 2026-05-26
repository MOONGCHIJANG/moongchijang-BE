package com.moongchijang.domain.owner.application.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "사장님 공구 마감 사유")
enum class OwnerGroupBuyCloseReasonType {
    SOLD_OUT,
    STORE_CONDITION,
    OTHER
}
