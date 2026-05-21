package com.moongchijang.domain.groupbuy.domain.entity

import com.moongchijang.global.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "group_buy_requests")
class GroupBuyRequest(

    @Column(nullable = false)
    val userId: Long,

    @Column(nullable = false, length = 100)
    val storeName: String,

    @Column(length = 200)
    val storeAddress: String? = null,

    @Column(name = "place_id", length = 100)
    val placeId: String? = null,

    @Column(name = "road_address", length = 200)
    val roadAddress: String? = null,

    @Column(name = "lot_address", length = 200)
    val lotAddress: String? = null,

    @Column
    val latitude: Double? = null,

    @Column
    val longitude: Double? = null,

    @Column(nullable = false, length = 100)
    val productName: String,

    @Column(nullable = false)
    val desiredQuantity: Int,

    @Column(nullable = false)
    val desiredPickupDate: LocalDate,

    @Column(length = 500)
    val additionalNote: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: GroupBuyRequestStatus = GroupBuyRequestStatus.IN_REVIEW,

    @Column(length = 20)
    var contactPhone: String? = null,

    @Column(length = 50)
    var contactInstagram: String? = null,

    @Column
    var rejectionReason: String? = null,

    @Column
    var openedGroupBuyId: Long? = null,

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L

) : BaseEntity()
