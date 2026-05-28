package com.moongchijang.domain.owner.application.dto

import com.moongchijang.domain.owner.domain.entity.OwnerGroupBuyRequestStatus

data class OwnerGroupBuyRequestCreateResponse(
    val requestId: Long,
    val status: OwnerGroupBuyRequestStatus
)
