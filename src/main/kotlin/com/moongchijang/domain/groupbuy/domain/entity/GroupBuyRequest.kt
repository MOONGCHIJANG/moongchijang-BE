package com.moongchijang.domain.groupbuy.domain.entity

import com.moongchijang.domain.user.domain.entity.User
import com.moongchijang.global.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "group_buy_requests")
class GroupBuyRequest(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

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

    @Column(length = 500)
    val additionalNote: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: GroupBuyRequestStatus = GroupBuyRequestStatus.REQUEST_RECEIVED,

    @Column
    var rejectionReason: String? = null,

    @Column
    var openedGroupBuyId: Long? = null,

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L

) : BaseEntity() {
    @Deprecated(
        message = "희망 수량과 희망 픽업일은 입점 요청에 사용하지 않습니다.",
        level = DeprecationLevel.WARNING
    )
    constructor(
        user: User,
        storeName: String,
        storeAddress: String? = null,
        placeId: String? = null,
        roadAddress: String? = null,
        lotAddress: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        productName: String,
        @Suppress("UNUSED_PARAMETER") desiredQuantity: Int,
        @Suppress("UNUSED_PARAMETER") desiredPickupDate: java.time.LocalDate,
        additionalNote: String? = null,
        status: GroupBuyRequestStatus = GroupBuyRequestStatus.REQUEST_RECEIVED,
        rejectionReason: String? = null,
        openedGroupBuyId: Long? = null,
        id: Long = 0L,
    ) : this(
        user = user,
        storeName = storeName,
        storeAddress = storeAddress,
        placeId = placeId,
        roadAddress = roadAddress,
        lotAddress = lotAddress,
        latitude = latitude,
        longitude = longitude,
        productName = productName,
        additionalNote = additionalNote,
        status = status,
        rejectionReason = rejectionReason,
        openedGroupBuyId = openedGroupBuyId,
        id = id,
    )

    fun markRejected(reason: String?) {
        status = GroupBuyRequestStatus.UNAVAILABLE
        rejectionReason = reason?.ifBlank { null }
    }
}
