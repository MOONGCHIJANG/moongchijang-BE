package com.moongchijang.domain.payment.application.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

data class CompletePortOnePaymentRequest(
    @field:NotBlank
    val paymentId: String,
    @field:Min(1)
    val amount: Int,
)
