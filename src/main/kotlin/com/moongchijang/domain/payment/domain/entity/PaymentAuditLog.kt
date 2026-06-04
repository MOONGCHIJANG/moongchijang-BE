package com.moongchijang.domain.payment.domain.entity

import com.moongchijang.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.ConstraintMode
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(
    name = "payment_audit_logs",
    indexes = [
        Index(name = "idx_payment_audit_logs_order_id", columnList = "order_id"),
        Index(name = "idx_payment_audit_logs_payment_order_id", columnList = "payment_order_id"),
        Index(name = "idx_payment_audit_logs_event_created", columnList = "event_type, created_at")
    ]
)
class PaymentAuditLog(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "payment_order_id",
        foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    var paymentOrder: PaymentOrder? = null,

    @Column(name = "order_id", length = 64)
    var orderId: String? = null,

    @Column(name = "pg_payment_id", length = 200)
    var pgPaymentId: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    var eventType: PaymentAuditEventType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var source: PaymentAuditSource,

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_order_status", length = 30)
    var previousOrderStatus: PaymentOrderStatus? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "current_order_status", length = 30)
    var currentOrderStatus: PaymentOrderStatus? = null,

    @Column(name = "pg_status", length = 50)
    var pgStatus: String? = null,

    @Column(length = 500)
    var reason: String? = null,

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    var rawPayload: String? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L
) : BaseEntity()

enum class PaymentAuditEventType {
    COMPLETE_REQUEST_RECEIVED,
    WEBHOOK_RECEIVED,
    PORTONE_STATUS_FETCHED,
    PAYMENT_APPROVED,
    PAYMENT_CANCELLED,
    PAYMENT_PARTIAL_CANCELLED,
    PAYMENT_FAILED,
    PAYMENT_IGNORED,
}

enum class PaymentAuditSource {
    COMPLETE_API,
    WEBHOOK,
    CANCEL_API,
    PENDING_REFUND,
}
