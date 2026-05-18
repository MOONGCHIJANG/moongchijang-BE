package com.moongchijang.domain.payment.domain.entity

import com.moongchijang.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "payments",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_payments_pg_payment_id", columnNames = ["pg_payment_id"]),
        UniqueConstraint(name = "uk_payments_payment_order_id", columnNames = ["payment_order_id"])
    ],
    indexes = [Index(name = "idx_payments_order_id", columnList = "order_id")]
)
class Payment(
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_order_id", nullable = false)
    var paymentOrder: PaymentOrder,

    @Column(name = "pg_payment_id", nullable = false, length = 200)
    var pgPaymentId: String,

    @Column(name = "order_id", nullable = false, length = 64)
    var orderId: String,

    @Column(nullable = false)
    var amount: Int,

    @Column(length = 50)
    var method: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: PaymentStatus = PaymentStatus.APPROVED,

    @Column(name = "approved_at", nullable = false)
    var approvedAt: LocalDateTime,

    @Column(name = "cancelled_at")
    var cancelledAt: LocalDateTime? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L
) : BaseEntity() {
    fun cancel(cancelledAt: LocalDateTime) {
        this.status = PaymentStatus.CANCELLED
        this.cancelledAt = cancelledAt
    }

    fun partialCancel(cancelledAt: LocalDateTime) {
        this.status = PaymentStatus.PARTIAL_CANCELLED
        this.cancelledAt = cancelledAt
    }
}
