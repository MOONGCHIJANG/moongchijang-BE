package com.moongchijang.domain.user.domain.entity

import com.moongchijang.domain.refund.domain.entity.RefundRequestStatus
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
    name = "withdrawn_refund_requests",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uidx_withdrawn_refund_requests_original_refund_request_id",
            columnNames = ["original_refund_request_id"],
        ),
    ],
    indexes = [
        Index(name = "idx_withdrawn_refund_requests_withdrawn_user_id", columnList = "withdrawn_user_id"),
        Index(name = "idx_withdrawn_refund_requests_withdrawn_account_id", columnList = "withdrawn_account_id"),
        Index(name = "idx_withdrawn_refund_requests_retention_expires_at", columnList = "retention_expires_at"),
    ],
)
class WithdrawnRefundRequest(
    @Column(name = "withdrawn_user_id", nullable = false)
    var withdrawnUserId: Long,

    @Column(name = "withdrawn_account_id")
    var withdrawnAccountId: Long? = null,

    @Column(name = "original_refund_request_id", nullable = false)
    var originalRefundRequestId: Long,

    @Column(name = "original_participation_id", nullable = false)
    var originalParticipationId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: RefundRequestStatus,

    @Column(name = "requested_amount", nullable = false)
    var requestedAmount: Int,

    @Column(name = "approved_refund_amount")
    var approvedRefundAmount: Int? = null,

    @Column(name = "rejected_reason", length = 200)
    var rejectedReason: String? = null,

    @Column(name = "requested_at", nullable = false)
    var requestedAt: LocalDateTime,

    @Column(name = "approved_at")
    var approvedAt: LocalDateTime? = null,

    @Column(name = "rejected_at")
    var rejectedAt: LocalDateTime? = null,

    @Column(name = "refunded_at")
    var refundedAt: LocalDateTime? = null,

    @Column(name = "withdrawn_at", nullable = false)
    var withdrawnAt: LocalDateTime,

    @Column(name = "retention_expires_at", nullable = false)
    var retentionExpiresAt: LocalDateTime,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
) : BaseEntity()
