package com.moongchijang.domain.payment.application.dto

import java.time.LocalDate
import java.time.LocalTime

data class CheckoutInfoResponse(
    val groupBuyId: Long,
    val storeName: String,
    val productName: String,
    val thumbnailUrl: String?,
    val pickupDate: LocalDate,
    val pickupTimeStart: LocalTime,
    val pickupTimeEnd: LocalTime,
    val unitPrice: Int,
    val quantity: Int,
    val productAmount: Int,
    val feeAmount: Int,
    val totalAmount: Int,
    val remainingQuantity: Int,
)
