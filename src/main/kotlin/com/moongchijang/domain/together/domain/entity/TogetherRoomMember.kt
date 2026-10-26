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
    name = "together_room_members",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_together_room_members_room_user", columnNames = ["room_id", "user_id"]),
    ],
    indexes = [
        Index(name = "idx_together_room_members_user_status", columnList = "user_id,status"),
        Index(name = "idx_together_room_members_room_status", columnList = "room_id,status"),
    ],
)
class TogetherRoomMember(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    var room: TogetherRoom,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var role: TogetherRoomMemberRole,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: TogetherRoomMemberStatus = TogetherRoomMemberStatus.ACTIVE,

    @Column(nullable = false)
    var quantity: Int,

    @Column(nullable = false)
    var amount: Int,

    @Column(name = "depositor_name", length = 50)
    var depositorName: String? = null,

    @Column(name = "final_confirmed_at")
    var finalConfirmedAt: LocalDateTime? = null,

    @Column(name = "cancelled_at")
    var cancelledAt: LocalDateTime? = null,

    @Column(name = "received_at")
    var receivedAt: LocalDateTime? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,
) : BaseEntity()
