package com.moongchijang.domain.owner.domain.entity

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
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
    name = "owner_group_buy_requests",
    indexes = [
        Index(name = "idx_owner_gbr_owner_id", columnList = "owner_id"),
        Index(name = "idx_owner_gbr_store_id", columnList = "store_id"),
        Index(name = "idx_owner_gbr_status", columnList = "status"),
        Index(name = "idx_owner_gbr_created_at", columnList = "created_at")
    ]
)
class OwnerGroupBuyRequest(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    var owner: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    var store: Store,

    @Column(name = "product_name", nullable = false, length = 100)
    var productName: String,

    @Column(name = "product_description", nullable = false, columnDefinition = "TEXT")
    var productDescription: String,

    @Column(name = "original_price")
    var originalPrice: Int? = null,

    @Column(nullable = false)
    var price: Int,

    @Column(name = "target_quantity", nullable = false)
    var targetQuantity: Int,

    @Column(name = "max_quantity", nullable = false)
    var maxQuantity: Int,

    @Column(name = "per_user_limit")
    var perUserLimit: Int? = null,

    @Column(name = "thumbnail_key", length = 500)
    var thumbnailKey: String? = null,

    @Column(nullable = false)
    var deadline: LocalDateTime,

    @Column(name = "pickup_date", nullable = false)
    var pickupDate: LocalDate,

    @Column(name = "pickup_time_start", nullable = false)
    var pickupTimeStart: LocalTime,

    @Column(name = "pickup_time_end", nullable = false)
    var pickupTimeEnd: LocalTime,

    @Column(name = "pickup_location", nullable = false, length = 200)
    var pickupLocation: String,

    @Column(name = "pickup_contact", length = 20)
    var pickupContact: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: OwnerGroupBuyRequestStatus = OwnerGroupBuyRequestStatus.PENDING,

    @Column(name = "rejection_reason", length = 200)
    var rejectionReason: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_group_buy_id")
    var approvedGroupBuy: GroupBuy? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    var reviewedBy: User? = null,

    @Column(name = "reviewed_at")
    var reviewedAt: LocalDateTime? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L
) : BaseEntity()
