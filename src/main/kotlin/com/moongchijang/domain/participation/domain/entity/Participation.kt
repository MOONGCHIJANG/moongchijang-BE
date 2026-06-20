package com.moongchijang.domain.participation.domain.entity

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.user.domain.entity.User
import com.moongchijang.global.entity.BaseEntity
import com.moongchijang.global.time.TimePolicy
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
    name = "participation",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_participation_user_group_buy",
            columnNames = ["user_id", "group_buy_id"]
        ),
        UniqueConstraint(
            name = "uk_participation_pickup_token",
            columnNames = ["pickup_token"]
        )
    ],
    indexes = [
        Index(name = "idx_participation_user_id", columnList = "user_id"),
        Index(name = "idx_participation_group_buy_id", columnList = "group_buy_id")
    ]
)
class Participation(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_buy_id", nullable = false)
    var groupBuy: GroupBuy,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pickup_processed_by")
    var pickupProcessedBy: User? = null,

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
    var status: ParticipationStatus = ParticipationStatus.PENDING,

    @Enumerated(EnumType.STRING)
    @Column(name = "pickup_status", nullable = false, length = 30)
    var pickupStatus: PickupStatus = PickupStatus.NOT_READY,

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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L
) : BaseEntity() {
    fun markPickedUp(
        processedBy: User,
        pickedUpAt: LocalDateTime = LocalDateTime.now(TimePolicy.STORAGE_ZONE_ID)
    ) {
        pickupProcessedBy = processedBy
        pickupStatus = PickupStatus.PICKED_UP
        this.pickedUpAt = pickedUpAt
    }
}
