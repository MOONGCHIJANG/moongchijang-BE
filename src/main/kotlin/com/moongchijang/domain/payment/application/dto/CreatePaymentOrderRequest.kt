package com.moongchijang.domain.payment.application.dto

import jakarta.validation.constraints.Min

data class CreatePaymentOrderRequest(
    @field:Min(1)
    val quantity: Int,
    val agreedNoCancelAfterGoal: Boolean,
    val agreedRefundBeforeGoal: Boolean,
    val agreedNoRefundAfterNoShow: Boolean,
    val agreedNoWithdrawal: Boolean,
)
