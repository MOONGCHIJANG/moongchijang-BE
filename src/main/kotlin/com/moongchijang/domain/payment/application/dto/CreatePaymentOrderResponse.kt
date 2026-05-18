package com.moongchijang.domain.payment.application.dto

data class CreatePaymentOrderResponse(
    val paymentId: String,
    val storeId: String,
    val channelKey: String,
    val orderName: String,
    val amount: Int,
    val customerName: String?,
)
