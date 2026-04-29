package com.moongchijang.domain.groupbuy.entity

import com.moongchijang.global.BaseEntity
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
    var status: GroupBuyRequestStatus = GroupBuyRequestStatus.SUBMITTED,

    @Column
    var rejectionReason: String? = null,

    @Column
    var openedGroupBuyId: Long? = null,

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L

) : BaseEntity()
