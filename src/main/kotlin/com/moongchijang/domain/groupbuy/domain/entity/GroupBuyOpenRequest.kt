package com.moongchijang.domain.groupbuy.domain.entity

import com.moongchijang.domain.user.domain.entity.User
import com.moongchijang.global.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "group_buy_open_requests",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_open_req_user_region_product",
            columnNames = ["user_id", "region", "product_name"]
        )
    ]
)
class GroupBuyOpenRequest(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(nullable = false, length = 50)
    val region: String,

    @Column(name = "product_name", nullable = false, length = 100)
    val productName: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_status", nullable = false, length = 20)
    var notificationStatus: NotificationStatus = NotificationStatus.PENDING,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L
) : BaseEntity() {
    fun markSent() {
        notificationStatus = NotificationStatus.SENT
    }

    fun markFailed() {
        notificationStatus = NotificationStatus.FAILED
    }
}
