package com.moongchijang.domain.groupbuy.domain.repository

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyOpenRequest
import com.moongchijang.domain.groupbuy.domain.entity.NotificationStatus
import org.springframework.data.jpa.repository.JpaRepository

interface GroupBuyOpenRequestRepository : JpaRepository<GroupBuyOpenRequest, Long> {
    fun existsByUserIdAndRegionAndProductName(userId: Long, region: String, productName: String): Boolean

    fun findAllByRegionAndProductNameAndNotificationStatus(
        region: String,
        productName: String,
        notificationStatus: NotificationStatus,
    ): List<GroupBuyOpenRequest>

    fun findAllByRegionInAndProductNameAndNotificationStatus(
        regions: Collection<String>,
        productName: String,
        notificationStatus: NotificationStatus,
    ): List<GroupBuyOpenRequest>
}
