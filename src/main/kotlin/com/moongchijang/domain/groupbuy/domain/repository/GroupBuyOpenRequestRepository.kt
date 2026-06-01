package com.moongchijang.domain.groupbuy.domain.repository

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyOpenRequest
import com.moongchijang.domain.groupbuy.domain.entity.NotificationStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface GroupBuyOpenRequestRepository : JpaRepository<GroupBuyOpenRequest, Long> {
    fun existsByUser_IdAndRegionAndProductName(userId: Long, region: String, productName: String): Boolean

    fun findAllByRegionAndProductNameAndNotificationStatus(
        region: String,
        productName: String,
        notificationStatus: NotificationStatus,
    ): List<GroupBuyOpenRequest>

    @Query(
        """
        select r
        from GroupBuyOpenRequest r
        join fetch r.user u
        where r.region in :regions
          and r.productName = :productName
          and r.notificationStatus = :notificationStatus
          and u.deletedAt is null
        """
    )
    fun findAllByRegionInAndProductNameAndNotificationStatus(
        @Param("regions") regions: Collection<String>,
        @Param("productName") productName: String,
        @Param("notificationStatus") notificationStatus: NotificationStatus,
    ): List<GroupBuyOpenRequest>
}
