package com.moongchijang.domain.csticket.domain.entity

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
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "cs_tickets")
class CsTicket(

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var type: CsTicketType,

    @Column(nullable = false, length = 100)
    var title: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var description: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var priority: CsTicketPriority = CsTicketPriority.MEDIUM,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: CsTicketStatus = CsTicketStatus.RECEIVED,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consumer_id")
    var consumer: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_buy_id")
    var groupBuy: GroupBuy? = null,

    @Column(name = "refund_participation_id")
    var refundParticipationId: Long? = null,

    @Column(name = "assignee_name", length = 50)
    var assigneeName: String? = null,

    @Column(name = "processing_memo", length = 1000)
    var processingMemo: String? = null,

    @Column(name = "resolved_at")
    var resolvedAt: LocalDateTime? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,
) : BaseEntity() {

    fun updateProcessing(
        status: CsTicketStatus?,
        assigneeName: String?,
        processingMemo: String?,
        now: LocalDateTime,
    ) {
        status?.let {
            this.status = it
            resolvedAt = if (it == CsTicketStatus.COMPLETED) now else null
        }
        assigneeName?.let { this.assigneeName = it.takeIf { value -> value.isNotBlank() } }
        processingMemo?.let { this.processingMemo = it.takeIf { value -> value.isNotBlank() } }
    }
}
