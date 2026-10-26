package com.moongchijang.domain.together.domain.entity

import com.moongchijang.domain.product.domain.entity.Product
import com.moongchijang.domain.store.domain.entity.Store
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Entity
@Table(
    name = "together_rooms",
    indexes = [
        Index(name = "idx_together_rooms_product_status", columnList = "product_id,status"),
        Index(name = "idx_together_rooms_leader_status", columnList = "leader_user_id,status"),
        Index(name = "idx_together_rooms_pickup_status", columnList = "pickup_date,status,pickup_time_end"),
        Index(name = "idx_together_rooms_payment_deadline", columnList = "payment_deadline_at"),
    ],
)
class TogetherRoom(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    var product: Product,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    var store: Store,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leader_user_id", nullable = false)
    var leader: User,

    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false, length = 20)
    var roomType: TogetherRoomType = TogetherRoomType.TOGETHER,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: TogetherRoomStatus = TogetherRoomStatus.RECRUITING,

    @Column(name = "pickup_date", nullable = false)
    var pickupDate: LocalDate,

    @Column(name = "pickup_time_start", nullable = false)
    var pickupTimeStart: LocalTime,

    @Column(name = "pickup_time_end", nullable = false)
    var pickupTimeEnd: LocalTime,

    @Column(name = "payment_deadline_at", nullable = false)
    var paymentDeadlineAt: LocalDateTime,

    @Column(name = "settlement_deadline_at")
    var settlementDeadlineAt: LocalDateTime? = null,

    @Column(name = "current_quantity", nullable = false)
    var currentQuantity: Int,

    @Column(name = "total_amount", nullable = false)
    var totalAmount: Int,

    @Column(name = "locked_at")
    var lockedAt: LocalDateTime? = null,

    @Column(name = "paid_at")
    var paidAt: LocalDateTime? = null,

    @Column(name = "pickup_completed_at")
    var pickupCompletedAt: LocalDateTime? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,
) : BaseEntity()
