package com.moongchijang.domain.groupbuy.repository

import com.moongchijang.domain.groupbuy.entity.GroupBuyRequest
import org.springframework.data.jpa.repository.JpaRepository

interface GroupBuyRequestRepository : JpaRepository<GroupBuyRequest, Long> {
    fun findByUserIdOrderByCreatedAtDesc(userId: Long): List<GroupBuyRequest>
}
