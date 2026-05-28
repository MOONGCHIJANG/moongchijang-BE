package com.moongchijang.domain.payment.domain.entity

enum class PaymentOrderStatus {
    READY,
    APPROVED,
    FAILED,
    CANCELLED,
    PARTIAL_CANCELLED
}
