package com.moongchijang.domain.user.application.dto

import com.moongchijang.domain.user.domain.entity.WithdrawalReason
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

data class WithdrawRequest(

    @field:Schema(
        description = "탈퇴 사유 (선택)",
        example = "NO_DESIRED_GROUPBUY",
    )
    val reason: WithdrawalReason? = null,

    @field:Schema(
        description = "탈퇴 상세 사유 (기타 선택 시 입력)",
        example = "근처 다른 매장을 주로 이용해요.",
    )
    @field:Size(max = 500)
    val reasonDetail: String? = null,
)
