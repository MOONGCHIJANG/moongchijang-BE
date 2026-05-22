package com.moongchijang.domain.groupbuy.application.dto

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequestStatus
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class GroupBuyRequestStatusUpdateRequest(

    @field:NotNull(message = "변경할 상태는 필수입니다")
    val targetStatus: GroupBuyRequestStatus,

    @field:Size(max = 500, message = "거절 사유는 500자 이하이어야 합니다")
    val rejectionReason: String? = null,

    val openedGroupBuyId: Long? = null
)
