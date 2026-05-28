package com.moongchijang.domain.refund.domain.entity

import com.moongchijang.domain.participation.domain.entity.Participation
import com.moongchijang.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "refund_requests")
class RefundRequest(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participation_id", nullable = false)
    var participation: Participation,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: RefundRequestStatus = RefundRequestStatus.REQUESTED,

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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,
) : BaseEntity() {
    fun markApproved(approvedRefundAmount: Int, at: LocalDateTime) {
        status = RefundRequestStatus.APPROVED
        this.approvedRefundAmount = approvedRefundAmount
        approvedAt = at
        rejectedReason = null
        rejectedAt = null
    }

    fun markRejected(reason: String?, at: LocalDateTime) {
        status = RefundRequestStatus.REJECTED
        rejectedReason = reason
        rejectedAt = at
    }

    fun markCompleted(at: LocalDateTime) {
        status = RefundRequestStatus.COMPLETED
        refundedAt = at
    }
}
