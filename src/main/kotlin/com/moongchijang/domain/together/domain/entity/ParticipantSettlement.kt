package com.moongchijang.domain.together.domain.entity

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
    name = "participant_settlements",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_participant_settlements_member_id", columnNames = ["member_id"]),
    ],
    indexes = [
        Index(name = "idx_participant_settlements_participant_status", columnList = "participant_user_id,status"),
        Index(name = "idx_participant_settlements_room_status", columnList = "room_id,status"),
    ],
)
class ParticipantSettlement(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    var room: TogetherRoom,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    var member: TogetherRoomMember,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_user_id", nullable = false)
    var participant: User,

    @Column(nullable = false)
    var amount: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: ParticipantSettlementStatus = ParticipantSettlementStatus.PENDING_TRANSFER,

    @Column(name = "transfer_marked_at")
    var transferMarkedAt: LocalDateTime? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by")
    var confirmedBy: User? = null,

    @Column(name = "confirmed_at")
    var confirmedAt: LocalDateTime? = null,

    @Column(name = "unpaid_marked_at")
    var unpaidMarkedAt: LocalDateTime? = null,

    @Column(name = "reminder_count", nullable = false)
    var reminderCount: Int = 0,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,
) : BaseEntity()
