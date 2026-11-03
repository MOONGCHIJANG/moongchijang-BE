package com.moongchijang.domain.groupbuy.domain.entity

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
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "settlement_transfers",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_settlement_transfers_room_participation",
            columnNames = ["room_settlement_id", "participation_id"],
        ),
    ],
    indexes = [Index(name = "idx_settlement_transfers_status_submitted_at", columnList = "status,submitted_at")],
)
class SettlementTransfer(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_settlement_id", nullable = false)
    var roomSettlement: RoomSettlement,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participation_id", nullable = false)
    var participation: Participation,

    @Column(name = "expected_amount", nullable = false)
    var expectedAmount: Int,

    @Column(name = "deposit_name", nullable = false, length = 50)
    var depositName: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: SettlementTransferStatus = SettlementTransferStatus.PENDING,

    @Column(name = "submitted_at")
    var submittedAt: LocalDateTime? = null,

    @Column(name = "confirmed_at")
    var confirmedAt: LocalDateTime? = null,

    @Column(name = "unpaid_at")
    var unpaidAt: LocalDateTime? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,
) : BaseEntity()
