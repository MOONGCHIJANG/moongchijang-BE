package com.moongchijang.domain.user.domain.entity

import com.moongchijang.domain.participation.domain.entity.OwnerRefundReviewStatus
import com.moongchijang.domain.participation.domain.entity.ParticipationCancelReason
import com.moongchijang.domain.participation.domain.entity.ParticipationStatus
import com.moongchijang.domain.participation.domain.entity.PickupStatus
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
    name = "withdrawn_participations",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uidx_withdrawn_participations_original_participation_id",
            columnNames = ["original_participation_id"],
        ),
    ],
    indexes = [
        Index(name = "idx_withdrawn_participations_withdrawn_user_id", columnList = "withdrawn_user_id"),
        Index(name = "idx_withdrawn_participations_withdrawn_account_id", columnList = "withdrawn_account_id"),
        Index(name = "idx_withdrawn_participations_retention_expires_at", columnList = "retention_expires_at"),
    ],
)
class WithdrawnParticipation(
    @Column(name = "withdrawn_user_id", nullable = false)
    var withdrawnUserId: Long,

    @Column(name = "withdrawn_account_id")
    var withdrawnAccountId: Long? = null,

    @Column(name = "original_participation_id", nullable = false)
    var originalParticipationId: Long,

    @Column(name = "original_group_buy_id", nullable = false)
    var originalGroupBuyId: Long,

    @Column(name = "original_payment_order_id")
    var originalPaymentOrderId: Long? = null,

    @Column(nullable = false)
    var quantity: Int,

    @Column(name = "product_amount", nullable = false)
    var productAmount: Int,

    @Column(name = "fee_amount", nullable = false)
    var feeAmount: Int,

    @Column(name = "total_amount", nullable = false)
    var totalAmount: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: ParticipationStatus,

    @Enumerated(EnumType.STRING)
    @Column(name = "pickup_status", nullable = false, length = 30)
    var pickupStatus: PickupStatus,

    @Column(name = "pickup_token", length = 100)
    var pickupToken: String? = null,

    @Column(name = "picked_up_at")
    var pickedUpAt: LocalDateTime? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "cancel_reason", length = 50)
    var cancelReason: ParticipationCancelReason? = null,

    @Column(name = "cancel_reason_detail", length = 500)
    var cancelReasonDetail: String? = null,

    @Column(name = "cancelled_at")
    var cancelledAt: LocalDateTime? = null,

    @Column(name = "refunded_at")
    var refundedAt: LocalDateTime? = null,

    @Column(name = "approved_refund_amount")
    var approvedRefundAmount: Int? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_refund_review_status", length = 30)
    var ownerRefundReviewStatus: OwnerRefundReviewStatus? = null,

    @Column(name = "owner_refund_dispute_reason", length = 500)
    var ownerRefundDisputeReason: String? = null,

    @Column(name = "owner_refund_reviewed_at")
    var ownerRefundReviewedAt: LocalDateTime? = null,

    @Column(name = "withdrawn_at", nullable = false)
    var withdrawnAt: LocalDateTime,

    @Column(name = "retention_expires_at", nullable = false)
    var retentionExpiresAt: LocalDateTime,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
) : BaseEntity()
