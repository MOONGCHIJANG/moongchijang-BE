package com.moongchijang.domain.together.domain.entity

import com.moongchijang.domain.store.domain.entity.StoreStaff
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
    name = "pickup_qr_codes",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_pickup_qr_codes_room_id", columnNames = ["room_id"]),
        UniqueConstraint(name = "uk_pickup_qr_codes_qr_token", columnNames = ["qr_token"]),
        UniqueConstraint(name = "uk_pickup_qr_codes_reservation_number", columnNames = ["reservation_number"]),
    ],
    indexes = [
        Index(name = "idx_pickup_qr_codes_active_expiry", columnList = "active_from,expires_at"),
    ],
)
class PickupQrCode(

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    var room: TogetherRoom,

    @Column(name = "qr_token", nullable = false, length = 100)
    var qrToken: String,

    @Column(name = "reservation_number", nullable = false, length = 100)
    var reservationNumber: String,

    @Column(name = "active_from", nullable = false)
    var activeFrom: LocalDateTime,

    @Column(name = "used_at")
    var usedAt: LocalDateTime? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "used_by_store_staff_id")
    var usedByStoreStaff: StoreStaff? = null,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: LocalDateTime,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,
) : BaseEntity()
