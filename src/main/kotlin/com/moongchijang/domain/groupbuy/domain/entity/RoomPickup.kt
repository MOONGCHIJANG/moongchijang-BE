package com.moongchijang.domain.groupbuy.domain.entity

import com.moongchijang.domain.user.domain.entity.User
import com.moongchijang.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "room_pickups",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_room_pickups_group_buy_id", columnNames = ["group_buy_id"]),
        UniqueConstraint(name = "uk_room_pickups_qr_token", columnNames = ["qr_token"]),
    ],
    indexes = [Index(name = "idx_room_pickups_active_from_used_at", columnList = "active_from,used_at")],
)
class RoomPickup(
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_buy_id", nullable = false)
    var groupBuy: GroupBuy,

    @Column(name = "qr_token", nullable = false, length = 100)
    var qrToken: String,

    @Column(name = "active_from", nullable = false)
    var activeFrom: LocalDateTime,

    @Column(name = "used_at")
    var usedAt: LocalDateTime? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "used_by_user_id")
    var usedBy: User? = null,

    @Column(name = "no_show_at")
    var noShowAt: LocalDateTime? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,
) : BaseEntity()
