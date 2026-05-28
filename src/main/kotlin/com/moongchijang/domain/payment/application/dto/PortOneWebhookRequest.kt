package com.moongchijang.domain.payment.application.dto

data class PortOneWebhookRequest(
    val type: String? = null,
    val storeId: String? = null,
    val paymentId: String? = null,
)
