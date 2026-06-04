package com.moongchijang.domain.user.domain.entity

import com.moongchijang.domain.payment.domain.entity.PaymentOrderStatus
import com.moongchijang.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "withdrawn_payment_orders",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uidx_withdrawn_payment_orders_original_payment_order_id",
            columnNames = ["original_payment_order_id"],
        ),
    ],
    indexes = [
        Index(name = "idx_withdrawn_payment_orders_withdrawn_user_id", columnList = "withdrawn_user_id"),
        Index(name = "idx_withdrawn_payment_orders_withdrawn_account_id", columnList = "withdrawn_account_id"),
        Index(name = "idx_withdrawn_payment_orders_retention_expires_at", columnList = "retention_expires_at"),
    ],
)
class WithdrawnPaymentOrder(
    @Column(name = "withdrawn_user_id", nullable = false)
    var withdrawnUserId: Long,

    @Column(name = "withdrawn_account_id")
    var withdrawnAccountId: Long? = null,

    @Column(name = "original_payment_order_id", nullable = false)
    var originalPaymentOrderId: Long,

    @Column(name = "original_group_buy_id", nullable = false)
    var originalGroupBuyId: Long,

    @Column(name = "order_id", nullable = false, length = 64)
    var orderId: String,

    @Column(nullable = false)
    var quantity: Int,

    @Column(name = "product_amount", nullable = false)
    var productAmount: Int,

    @Column(name = "fee_amount", nullable = false)
    var feeAmount: Int,

    @Column(name = "total_amount", nullable = false)
    var totalAmount: Int,

    @Column(name = "agreed_no_cancel_after_goal", nullable = false)
    var agreedNoCancelAfterGoal: Boolean,

    @Column(name = "agreed_refund_before_goal", nullable = false)
    var agreedRefundBeforeGoal: Boolean,

    @Column(name = "agreed_no_refund_after_no_show", nullable = false)
    var agreedNoRefundAfterNoShow: Boolean,

    @Column(name = "agreed_no_withdrawal", nullable = false)
    var agreedNoWithdrawal: Boolean,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: PaymentOrderStatus,

    @Column(name = "approved_at")
    var approvedAt: LocalDateTime? = null,

    @Column(name = "failed_at")
    var failedAt: LocalDateTime? = null,

    @Column(name = "cancelled_at")
    var cancelledAt: LocalDateTime? = null,

    @Column(name = "withdrawn_at", nullable = false)
    var withdrawnAt: LocalDateTime,

    @Column(name = "retention_expires_at", nullable = false)
    var retentionExpiresAt: LocalDateTime,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
) : BaseEntity()
