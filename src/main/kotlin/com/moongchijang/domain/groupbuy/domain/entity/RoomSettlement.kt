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
    name = "room_settlements",
    uniqueConstraints = [UniqueConstraint(name = "uk_room_settlements_group_buy_id", columnNames = ["group_buy_id"])],
    indexes = [
        Index(name = "idx_room_settlements_organizer_user_id", columnList = "organizer_user_id"),
        Index(name = "idx_room_settlements_status_deadline", columnList = "status,settlement_deadline"),
    ],
)
class RoomSettlement(
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_buy_id", nullable = false)
    var groupBuy: GroupBuy,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_user_id", nullable = false)
    var organizer: User,

    @Column(name = "bank_code", nullable = false, length = 20)
    var bankCode: String,

    @Column(name = "account_number_encrypted", nullable = false)
    var accountNumberEncrypted: ByteArray,

    @Column(name = "account_holder_name_enc", nullable = false)
    var accountHolderNameEncrypted: ByteArray,

    @Column(name = "third_party_consent_at", nullable = false)
    var thirdPartyConsentAt: LocalDateTime,

    @Column(name = "settlement_deadline", nullable = false)
    var settlementDeadline: LocalDateTime,

    @Column(nullable = false, length = 20)
    var status: String,

    @Column(name = "closed_at")
    var closedAt: LocalDateTime? = null,

    @Column(name = "masked_at")
    var maskedAt: LocalDateTime? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,
) : BaseEntity()
