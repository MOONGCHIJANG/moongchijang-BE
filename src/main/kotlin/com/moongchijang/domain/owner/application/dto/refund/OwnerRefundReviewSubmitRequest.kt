package com.moongchijang.domain.owner.application.dto.refund

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

@Schema(description = "사장님 환불 요청 검토 제출 요청")
data class OwnerRefundReviewSubmitRequest(

    @field:NotNull(message = "검토 액션은 필수입니다.")
    @field:Schema(description = "검토 액션", example = "APPROVE")
    val action: OwnerRefundReviewActionType,

    @field:Size(max = 500, message = "의의 제기 사유는 500자 이하여야 합니다.")
    @field:Schema(description = "의의 제기 사유(action=DISPUTE일 때 입력)", example = "픽업 요청 시간 변경이 선행되어야 합니다.")
    val disputeReason: String? = null,
)
