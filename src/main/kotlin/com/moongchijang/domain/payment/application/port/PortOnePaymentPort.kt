package com.moongchijang.domain.payment.application.port

import java.time.LocalDateTime

interface PortOnePaymentPort {
    fun getPayment(paymentId: String): PortOnePaymentResult
}

data class PortOnePaymentResult(
    val paymentId: String,
    val status: String,
    val totalAmount: Int,
    val method: String?,
    val paidAt: LocalDateTime?,
    val cancelledAt: LocalDateTime? = null,
)
