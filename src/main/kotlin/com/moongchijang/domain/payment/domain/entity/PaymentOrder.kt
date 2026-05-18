package com.moongchijang.domain.payment.domain.entity

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.user.domain.entity.User
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
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "payment_orders",
    uniqueConstraints = [UniqueConstraint(name = "uk_payment_orders_order_id", columnNames = ["order_id"])],
    indexes = [
        Index(name = "idx_payment_orders_user_id", columnList = "user_id"),
        Index(name = "idx_payment_orders_group_buy_id", columnList = "group_buy_id")
    ]
)
class PaymentOrder(
    @Column(name = "order_id", nullable = false, length = 64)
    var orderId: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_buy_id", nullable = false)
    var groupBuy: GroupBuy,

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
    var status: PaymentOrderStatus = PaymentOrderStatus.READY,

    @Column(name = "approved_at")
    var approvedAt: LocalDateTime? = null,

    @Column(name = "failed_at")
    var failedAt: LocalDateTime? = null,

    @Column(name = "cancelled_at")
    var cancelledAt: LocalDateTime? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L
) : BaseEntity() {
    fun approve(approvedAt: LocalDateTime) {
        this.status = PaymentOrderStatus.APPROVED
        this.approvedAt = approvedAt
    }

    fun fail(failedAt: LocalDateTime) {
        this.status = PaymentOrderStatus.FAILED
        this.failedAt = failedAt
    }

    fun cancel(cancelledAt: LocalDateTime) {
        this.status = PaymentOrderStatus.CANCELLED
        this.cancelledAt = cancelledAt
    }

    fun partialCancel(cancelledAt: LocalDateTime) {
        this.status = PaymentOrderStatus.PARTIAL_CANCELLED
        this.cancelledAt = cancelledAt
    }
}
