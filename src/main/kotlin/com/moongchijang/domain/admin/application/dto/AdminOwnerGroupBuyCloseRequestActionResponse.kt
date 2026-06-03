package com.moongchijang.domain.admin.application.dto

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyCloseRequestReviewStatus
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "운영자 사장님 공구 마감 요청 처리 응답")
data class AdminOwnerGroupBuyCloseRequestActionResponse(
    @field:Schema(description = "공구 ID", example = "21")
    val groupBuyId: Long,

    @field:Schema(description = "마감 요청 검토 상태", example = "APPROVED")
    val reviewStatus: GroupBuyCloseRequestReviewStatus,

    @field:Schema(description = "처리 후 공구 상태", example = "CLOSED")
    val groupBuyStatus: GroupBuyStatus
)
