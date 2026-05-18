package com.moongchijang.domain.payment.domain.repository

import com.moongchijang.domain.payment.domain.entity.Payment
import org.springframework.data.jpa.repository.JpaRepository

interface PaymentRepository : JpaRepository<Payment, Long> {
    fun findByPaymentOrderOrderId(orderId: String): Payment?
}
