package com.moongchijang.domain.payment.experiment

data class PaymentExperimentPreparationResponse(
    val requests: List<PreparedPaymentRequestPayload>,
)

data class PreparedPaymentRequestPayload(
    val userId: Long,
    val email: String,
    val accessToken: String,
    val paymentId: String,
    val amount: Int,
)
