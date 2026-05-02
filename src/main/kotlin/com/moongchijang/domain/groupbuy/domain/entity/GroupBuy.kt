package com.moongchijang.domain.groupbuy.domain.entity

import com.moongchijang.domain.store.domain.entity.Store
import com.moongchijang.global.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalTime

@Entity
@Table(name = "group_buys")
class GroupBuy(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    var store: Store,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_buy_request_id", nullable = false)
    var groupBuyRequest: GroupBuyRequest,

    @Column(length = 500)
    var thumbnailUrl: String? = null,

    @Column(nullable = false, length = 100)
    var productName: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var productDescription: String,

    @Column(nullable = false)
    var price: Int,

    @Column(nullable = false)
    var targetQuantity: Int,

    @Column(nullable = false)
    var currentQuantity: Int = 0,

    @Column(nullable = false)
    var maxQuantity: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: GroupBuyStatus,

    @Column(nullable = false)
    var deadline: LocalDate,

    @Column(nullable = false)
    var pickupDate: LocalDate,

    @Column(nullable = false)
    var pickupTimeStart: LocalTime,

    @Column(nullable = false)
    var pickupTimeEnd: LocalTime,

    @Column(nullable = false, length = 200)
    var pickupLocation: String,

    @Column(nullable = false)
    var shareCount: Int = 0,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L

) : BaseEntity()
