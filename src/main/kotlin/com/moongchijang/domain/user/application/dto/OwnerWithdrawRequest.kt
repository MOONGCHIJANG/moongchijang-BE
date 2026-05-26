package com.moongchijang.domain.user.application.dto

import com.moongchijang.domain.user.domain.entity.OwnerWithdrawalReason
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

data class OwnerWithdrawRequest(

    @field:Schema(
        description = "사장님 탈퇴 사유 (선택)",
        example = "INCONVENIENT_SERVICE",
    )
    val reason: OwnerWithdrawalReason? = null,

    @field:Schema(
        description = "사장님 탈퇴 상세 사유 (기타 선택 시 입력)",
        example = "정산/운영 기능 사용이 불편해요.",
    )
    @field:Size(max = 500)
    val reasonDetail: String? = null,
)
