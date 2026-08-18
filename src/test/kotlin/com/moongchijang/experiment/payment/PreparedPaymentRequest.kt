package com.moongchijang.experiment.payment

data class PreparedPaymentRequest(
    val accessToken: String,
    val paymentId: String,
    val amount: Int,
)
