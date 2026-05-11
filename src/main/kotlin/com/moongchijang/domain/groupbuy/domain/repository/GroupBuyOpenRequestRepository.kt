package com.moongchijang.domain.groupbuy.domain.repository

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyOpenRequest
import org.springframework.data.jpa.repository.JpaRepository

interface GroupBuyOpenRequestRepository : JpaRepository<GroupBuyOpenRequest, Long> {
    fun existsByUserIdAndRegionAndProductName(userId: Long, region: String, productName: String): Boolean
}
